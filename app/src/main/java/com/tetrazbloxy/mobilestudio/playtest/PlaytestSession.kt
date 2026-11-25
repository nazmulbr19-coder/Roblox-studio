package com.tetrazbloxy.mobilestudio.playtest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tetrazbloxy.mobilestudio.engine.CameraMath
import com.tetrazbloxy.mobilestudio.engine.CharacterPhysics
import com.tetrazbloxy.mobilestudio.engine.PlaytestConstants
import com.tetrazbloxy.mobilestudio.model.BasePart
import com.tetrazbloxy.mobilestudio.model.DataModel
import com.tetrazbloxy.mobilestudio.model.Humanoid
import com.tetrazbloxy.mobilestudio.model.SpawnLocation
import com.tetrazbloxy.mobilestudio.model.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class AvatarKind(val title: String, val description: String) {
    R15("R15", "Modern rig with 15 body parts"),
    R6("R6", "Classic six-part character"),
    RTHRO("Rthro", "Tall, stylised character")
}

enum class HudPage { NONE, MENU, PEOPLE, SETTINGS, HELP, CONFIRM }

data class ChatLine(
    val author: String,
    val text: String,
    val system: Boolean = false,
    val time: Long = System.currentTimeMillis()
)

data class PlayPerson(val name: String, val health: Float, val isYou: Boolean = false)

/**
 * In-process playtest runtime. It owns a cloned world, character physics,
 * third-person camera state, chat, people list and the frame loop.
 * No OS service or notification is involved, matching SPEC 3.2.
 */
class PlaytestSession(
    val world: DataModel,
    val avatar: AvatarKind,
    private val scope: CoroutineScope
) {
    var position by mutableStateOf(Vector3(0f, 0f, 0f))
        private set
    var facingYaw by mutableStateOf(0f)
        private set
    var cameraYaw by mutableStateOf(0f)
        private set
    var cameraPitch by mutableStateOf(-0.28f)
        private set
    var cameraTarget by mutableStateOf(Vector3(0f, 3f, 0f))
        private set
    var velocityY by mutableStateOf(0f)
        private set
    var grounded by mutableStateOf(false)
        private set
    var health by mutableStateOf(100f)
        private set
    var maxHealth by mutableStateOf(100f)
        private set
    var walkSpeed by mutableStateOf(PlaytestConstants.DEFAULT_WALK_SPEED)
        private set
    var jumpPower by mutableStateOf(PlaytestConstants.DEFAULT_JUMP_POWER)
        private set
    var useJumpPower by mutableStateOf(true)
        private set
    var jumpHeight by mutableStateOf(PlaytestConstants.DEFAULT_JUMP_HEIGHT)
        private set
    var forwardInput by mutableStateOf(0f)
        private set
    var strafeInput by mutableStateOf(0f)
        private set
    var sprinting by mutableStateOf(false)
        private set
    var fps by mutableStateOf(60)
        private set
    var elapsed by mutableStateOf(0f)
        private set
    var hudPage by mutableStateOf(HudPage.NONE)
        private set
    var chatOpen by mutableStateOf(false)
        private set
    var chatDraft by mutableStateOf("")
        private set
    var cameraDistance by mutableStateOf(11f)
        private set
    var respawns by mutableStateOf(0)
        private set

    val chat = mutableStateListOf<ChatLine>()
    val people = mutableStateListOf<PlayPerson>()

    private var job: Job? = null
    private var spawn = Vector3(0f, 0f, 0f)
    private var groundY = 0f
    private var jumpAnimation = 0f

    init {
        configureFromWorld()
        resetCharacter()
        people.add(PlayPerson("You", 100f, isYou = true))
        people.add(PlayPerson("Builder", 100f))
        people.add(PlayPerson("Tester", 100f))
        people.add(PlayPerson("Guest 2048", 100f))
        chat.add(ChatLine("", "Welcome to Mobile Studio playtest. Type /help for commands.", system = true))
        chat.add(ChatLine("Builder", "This baseplate looks great!"))
    }

    private fun configureFromWorld() {
        val spawnPart = world.workspace.getDescendants()
            .filterIsInstance<SpawnLocation>()
            .firstOrNull()
            ?: world.workspace.getDescendants().filterIsInstance<BasePart>()
                .firstOrNull { it.name.contains("spawn", true) }
        if (spawnPart != null) {
            spawn = spawnPart.cframe.position + Vector3(0f, spawnPart.size.y * 0.5f + 0.05f, 0f)
            groundY = spawnPart.cframe.position.y + spawnPart.size.y * 0.5f
        } else {
            val baseplate = world.workspace.getDescendants()
                .filterIsInstance<BasePart>()
                .maxByOrNull { it.size.x * it.size.z }
            if (baseplate != null) {
                groundY = baseplate.cframe.position.y + baseplate.size.y * 0.5f
                spawn = Vector3(baseplate.cframe.position.x, groundY + 0.05f, baseplate.cframe.position.z)
            }
        }

        val humanoid = world.workspace.getDescendants()
            .filterIsInstance<Humanoid>()
            .firstOrNull()
        if (humanoid != null) {
            maxHealth = humanoid.maxHealth.coerceAtLeast(1f)
            health = humanoid.health.coerceIn(0f, maxHealth)
            walkSpeed = humanoid.walkSpeed
            jumpPower = humanoid.jumpPower
            useJumpPower = humanoid.useJumpPower
            jumpHeight = humanoid.jumpHeight
        }
    }

    fun start() {
        if (job != null) return
        job = scope.launch {
            var last = System.nanoTime()
            var smoothed = 60f
            while (isActive) {
                val now = System.nanoTime()
                val raw = ((now - last) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.1f)
                last = now
                step(raw)
                smoothed += (1f / raw - smoothed) * 0.08f
                fps = smoothed.toInt().coerceIn(1, 240)
                delay(16)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun step(rawDt: Float) {
        val dt = CharacterPhysics.clampDt(rawDt)
        elapsed += dt

        val direction = CharacterPhysics.moveDirection(cameraYaw, strafeInput, -forwardInput)
        val speed = walkSpeed * if (sprinting) 1.6f else 1f
        val horizontal = Vector3(
            position.x + direction.x * speed * dt,
            position.y,
            position.z + direction.z * speed * dt
        )
        position = horizontal

        if (!grounded) {
            velocityY = CharacterPhysics.applyGravity(velocityY, PlaytestConstants.DEFAULT_GRAVITY, dt)
        }

        var nextY = position.y + velocityY * dt
        if (nextY <= groundY) {
            nextY = groundY
            velocityY = 0f
            grounded = true
        } else {
            grounded = false
        }
        position = Vector3(position.x, nextY, position.z)

        if (abs(direction.x) + abs(direction.z) > 0.01f) {
            val targetYaw = kotlin.math.atan2(-direction.x, -direction.z)
            facingYaw = CameraMath.approach(facingYaw, targetYaw, 12f, dt)
        }

        cameraTarget = cameraTarget.lerp(position + Vector3(0f, 3f, 0f), (dt * 10f).coerceIn(0f, 1f))
        cameraDistance = CameraMath.approach(
            cameraDistance,
            if (health <= 0f) 9f else 11f,
            4f,
            dt
        )

        if (jumpAnimation > 0f) jumpAnimation = (jumpAnimation - dt).coerceAtLeast(0f)
    }

    fun move(forward: Float, strafe: Float) {
        forwardInput = forward.coerceIn(-1f, 1f)
        strafeInput = strafe.coerceIn(-1f, 1f)
    }

    fun releaseMove() {
        forwardInput = 0f
        strafeInput = 0f
    }

    fun jump() {
        if (!grounded) return
        velocityY = CharacterPhysics.jumpVelocity(jumpPower, useJumpPower, jumpHeight)
        grounded = false
        jumpAnimation = PlaytestConstants.JUMP_ANIM_TIME
    }

    fun setSprinting(value: Boolean) {
        sprinting = value
    }

    fun look(deltaYaw: Float, deltaPitch: Float) {
        cameraYaw = CameraMath.wrapAngle(cameraYaw + deltaYaw)
        cameraPitch = (cameraPitch + deltaPitch).coerceIn(-1.35f, 1.25f)
    }

    fun zoom(delta: Float) {
        cameraDistance = (cameraDistance + delta).coerceIn(4f, 42f)
    }

    fun resetCharacter() {
        position = spawn
        velocityY = 0f
        grounded = false
        health = maxHealth
        cameraYaw = 0f
        cameraPitch = -0.28f
        cameraTarget = spawn + Vector3(0f, 3f, 0f)
        respawns++
        people[0] = PlayPerson("You", health, isYou = true)
    }

    fun damage(amount: Float) {
        health = (health - amount).coerceAtLeast(0f)
        people[0] = PlayPerson("You", health, isYou = true)
        if (health <= 0f) {
            chat.add(ChatLine("", "You died. Respawning…", system = true))
            resetCharacter()
        }
    }

    fun sendChat(text: String) {
        val message = text.trim()
        if (message.isEmpty()) return
        if (message.startsWith("/")) {
            command(message)
            return
        }
        chat.add(ChatLine("You", message))
        capChat()
        if (message.contains("hello", true) || message.contains("hi", true)) {
            scope.launch {
                delay(900)
                chat.add(ChatLine("Builder", "Hey @You!"))
                capChat()
            }
        }
    }

    private fun command(message: String) {
        when (message.lowercase()) {
            "/help" -> chat.add(
                ChatLine(
                    "",
                    "Commands: /reset, /clear, /zoom <4-42>, /speed <1-100>, /help",
                    system = true
                )
            )
            "/reset" -> {
                resetCharacter()
                chat.add(ChatLine("", "Character reset.", system = true))
            }
            "/clear" -> chat.clear()
            else -> {
                val parts = message.split(' ')
                when (parts.firstOrNull()?.lowercase()) {
                    "/zoom" -> {
                        val value = parts.getOrNull(1)?.toFloatOrNull()
                        if (value != null) {
                            cameraDistance = value.coerceIn(4f, 42f)
                            chat.add(ChatLine("", "Camera distance ${cameraDistance.toInt()}", system = true))
                        }
                    }
                    "/speed" -> {
                        val value = parts.getOrNull(1)?.toFloatOrNull()
                        if (value != null) {
                            walkSpeed = value.coerceIn(1f, 100f)
                            chat.add(ChatLine("", "Walk speed ${walkSpeed.toInt()}", system = true))
                        }
                    }
                    else -> chat.add(ChatLine("", "Unknown command. Try /help.", system = true))
                }
            }
        }
        capChat()
    }

    fun updateDraft(value: String) {
        chatDraft = value
    }

    fun openChat() {
        chatOpen = true
    }

    fun closeChat() {
        chatOpen = false
        chatDraft = ""
    }

    fun showPage(page: HudPage) {
        hudPage = page
    }

    fun hidePage() {
        hudPage = HudPage.NONE
    }

    fun setHealth(value: Float) {
        health = value.coerceIn(0f, maxHealth)
        people[0] = PlayPerson("You", health, isYou = true)
    }

    fun setWalkSpeed(value: Float) {
        walkSpeed = value.coerceIn(1f, 100f)
    }

    fun setJumpPower(value: Float) {
        jumpPower = value.coerceIn(1f, 500f)
    }

    fun setCameraDistance(value: Float) {
        cameraDistance = value.coerceIn(4f, 42f)
    }

    private fun capChat() {
        while (chat.size > 100) chat.removeAt(0)
    }
}
