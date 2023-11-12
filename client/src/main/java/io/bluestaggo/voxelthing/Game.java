package io.bluestaggo.voxelthing;

import io.bluestaggo.pds.CompoundItem;
import io.bluestaggo.voxelthing.assets.Texture;
import io.bluestaggo.voxelthing.gui.screen.*;
import io.bluestaggo.voxelthing.renderer.MainRenderer;
import io.bluestaggo.voxelthing.renderer.draw.Quad;
import io.bluestaggo.voxelthing.settings.Settings;
import io.bluestaggo.voxelthing.util.OperatingSystem;
import io.bluestaggo.voxelthing.window.ClientPlayerController;
import io.bluestaggo.voxelthing.window.Window;
import io.bluestaggo.voxelthing.world.BlockRaycast;
import io.bluestaggo.voxelthing.world.ClientWorld;
import io.bluestaggo.voxelthing.world.World;
import io.bluestaggo.voxelthing.world.WorldInfo;
import io.bluestaggo.voxelthing.world.block.Block;
import io.bluestaggo.voxelthing.world.chunk.Chunk;
import io.bluestaggo.voxelthing.world.entity.IPlayerController;
import io.bluestaggo.voxelthing.world.entity.Player;
import io.bluestaggo.voxelthing.world.storage.FolderSaveHandler;
import io.bluestaggo.voxelthing.world.storage.ISaveHandler;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.glClearColor;

public class Game {
	public static final String VERSION;
	public static final int TICKS_PER_SECOND = 20;
	public static final float TICK_RATE = 1.0f / TICKS_PER_SECOND;

	static {
		String version = "???";

		try (InputStream stream = Game.class.getResourceAsStream("/version.txt")) {
			if (stream == null) {
				version = "dev " + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "?";
			} else {
				var reader = new BufferedReader(new InputStreamReader(stream));
				version = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		VERSION = version;
	}

	public static final String[] SKINS = {
			"joel",
			"staggo",
			"floof",
			"talon",
			"kinny"
	};

	private static Game instance;

	public final Path saveDir;
	public final Path worldDir;

	public final Window window;
	public final MainRenderer renderer;
	public final Settings settings;

	public World world;
	public Player player;
	public IPlayerController playerController;

	public Block[] palette = new Block[9];
	public int heldItem;

	private final GuiScreen debugGui;
	private final GuiScreen inGameGui;
	private GuiScreen currentGui;
	private boolean debugMenu = true;

	private BlockRaycast blockRaycast;

	public float mouseSensitivity = 0.25f;

	private double tickTime;
	private double partialTick;

	public Game() {
		instance = this;

		saveDir = getSaveDir();
		worldDir = saveDir.resolve("worlds");

		window = new Window();
		window.grabCursor();

		settings = new Settings();
		settings.readFrom(saveDir.resolve("settings.dat"));

		renderer = new MainRenderer(this);

		debugGui = new DebugGui(this);
		inGameGui = new IngameGui(this);

		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		openGui(new MainMenu(this));
	}

	public static Game getInstance() {
		return instance;
	}

	private static Path getSaveDir() {
		String home = System.getProperty("user.home", ".");
		Path dir = switch (OperatingSystem.get()) {
			case WINDOWS -> Path.of(Optional.ofNullable(System.getenv("APPDATA")).orElse(home), "VoxelThing");
			case MACOS -> Path.of(home, "Library", "Application Support", "Voxel Thing");
			default -> Path.of(home, ".voxelthing");
		};

		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create Voxel Thing data directory!", e);
		}

		return dir;
	}

	public void run() {
		while (!window.shouldClose()) {
			try {
				update(window.getDeltaTime());
				draw();
				window.update();
				limitFps();
			} catch (Throwable e) {
				e.printStackTrace();
				var stackTrace = new StringWriter();
				var stackTracePrinter = new PrintWriter(stackTrace);
				stackTracePrinter.println("An exception has occured!");
				e.printStackTrace(stackTracePrinter);
				stackTracePrinter.println("Continue execution?");
				boolean stop = JOptionPane.showConfirmDialog(null, stackTrace, "Voxel Thing: Exception", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) != 1;

				if (!stop) {
					break;
				}
			}
		}

		close();
	}

	private void close() {
		exitWorld();
		renderer.unload();
		window.destroy();
	}

	public void startWorld() {
		startWorld((ISaveHandler) null, null);
	}

	public void startWorld(ISaveHandler saveHandler) {
		startWorld(saveHandler, null);
	}

	public void startWorld(String name, WorldInfo worldInfo) {
		ISaveHandler saveHandler = null;
		if (name != null) {
			try {
				saveHandler = new FolderSaveHandler(worldDir.resolve(name));
			} catch (IOException e) {
				System.out.println("Cannot save world \"" + name + "\"! Playing without saving.");
				e.printStackTrace();
			}
		}

		startWorld(saveHandler, worldInfo);
	}

	public void startWorld(ISaveHandler saveHandler, WorldInfo worldInfo) {
		exitWorld();

		world = new ClientWorld(this, saveHandler, worldInfo);
		saveHandler = world.saveHandler;
		playerController = new ClientPlayerController(this);
		player = new Player(world, playerController);

		CompoundItem playerData = saveHandler.loadData("player");
		if (playerData != null) {
			player.deserialize(playerData);
		}

		currentGui = null;
		window.grabCursor();
	}

	public void exitWorld() {
		if (world != null) {
			world.saveHandler.saveData("player", player.serialize());
			world.close();
			player = null;
			world = null;
		}

		System.gc();
	}

	public boolean isInWorld() {
		return world != null && player != null;
	}

	private void update(double delta) {
		tickTime += delta;
		GuiScreen gui = currentGui != null ? currentGui : isInWorld() ? inGameGui : null;

		if (gui == null) {
			gui = currentGui = new MainMenu(this);
			window.ungrabCursor();
		}

		if (gui == inGameGui) {
			doControls();
		}
		gui.handleInput();
		boolean paused = gui.pauseGame();

		if (isInWorld()) {
			if (paused) {
				partialTick = 1.0;
				world.partialTick = 1.0;
			} else {
				player.onGameUpdate();
				player.noClip = window.isKeyDown(GLFW_KEY_Q);
				renderer.worldRenderer.loadChunks(10);
			}
		}

		if (tickTime >= TICK_RATE) {
			tickTime %= TICK_RATE;
			if (currentGui != null) {
				currentGui.tick();
			}

			if (isInWorld() && !paused) {
				assert inGameGui != null;
				inGameGui.tick();
				player.tick();
				world.tick();
			}
		}

		if (!paused) {
			partialTick = tickTime / TICK_RATE;
		}

		if (isInWorld()) {
			world.partialTick = partialTick;

			double px = player.getPartialX();
			double py = player.getPartialY();
			double pz = player.getPartialZ();
			float yaw = (float) player.rotYaw;
			float pitch = (float) player.rotPitch;

			py += player.height - 0.3;
			if (settings.viewBobbing.getValue()) {
				if (settings.thirdPerson.getValue()) {
					py -= player.getPartialVelY() * 0.2;
				}
				pitch += player.getFallAmount() * 2.5;
			}

			renderer.camera.setPosition(0.0f, 0.0f, 0.0f);
			renderer.camera.setOffset(px, py, pz);
			renderer.camera.setRotation(yaw, pitch);

			if (world != null) {
				blockRaycast = renderer.camera.getRaycast(5.0f);
				world.doRaycast(blockRaycast);
			}

			if (settings.thirdPerson.getValue()) {
				renderer.camera.moveForward(-4.0f);
			} else if (settings.viewBobbing.getValue()) {
				renderer.camera.addPosition(0.0f, (float)Math.abs(player.getRenderWalk()) * 0.1f, 0.0f);
				renderer.camera.moveRight((float) player.getRenderWalk() * 0.05f);
			}
		}
	}

	private void doControls() {
		if (window.isKeyJustPressed(GLFW_KEY_R)) {
			player.posX = world.random.nextDouble(-1000.0, 1000.0);
			player.posY = 64.0;
			player.posZ = world.random.nextDouble(-1000.0, 1000.0);
			player.velX = 0.0;
			player.velY = 0.0;
			player.velZ = 0.0;
		}

		if (window.isKeyJustPressed(GLFW_KEY_F1)) {
			settings.hideGui.setValue(!settings.hideGui.getValue());
		}

		if (window.isKeyJustPressed(GLFW_KEY_F3)) {
			debugMenu = !debugMenu;
		}

		if (window.isKeyJustPressed(GLFW_KEY_F5)) {
			settings.thirdPerson.setValue(!settings.thirdPerson.getValue());
		}

		if (window.isKeyJustPressed(GLFW_KEY_ESCAPE)) {
			openGui(new PauseMenu(this));
		}

		if (window.isKeyJustPressed(GLFW_KEY_E)) {
			openGui(new BlockInventory(this));
		}
	}

	private void draw() {
		renderer.draw();

		if (isInWorld()) {
			if (!settings.hideGui.getValue()) {
				if (debugMenu) {
					debugGui.draw();
				}
				inGameGui.draw();
			}
		} else {
			Texture bgTex = renderer.textures.getTexture("/assets/gui/background.png");

			float width = renderer.screen.getWidth();
			float height = renderer.screen.getHeight();

			renderer.draw2D.drawQuad(Quad.shared()
					.size(width, height)
					.withTexture(bgTex)
					.withUV(0.0f, 0.0f, width / bgTex.width, height / bgTex.height)
			);
		}

		if (currentGui != null) {
			currentGui.draw();
		}
	}

	private void limitFps() {
		int opt = settings.limitFps.getValue();
		if (opt > 1 || opt < 0) {
			opt = currentGui == null ? 0 : 1;
		}
		glfwSwapInterval(opt);
	}

	public void openGui(GuiScreen gui) {
		GuiScreen oldGui = gui == null && gui != currentGui ? currentGui : null;
		if (gui == null) {
			if (currentGui.parent != null) {
				gui = currentGui.parent;
			} else if (!isInWorld()) {
				gui = new MainMenu(this);
			}
		}

		currentGui = gui;
		if (oldGui != null) {
			oldGui.onClosed();
		}

		if (gui == null) {
			window.grabCursor();
		} else {
			window.ungrabCursor();
		}
	}

	public void closeGui() {
		openGui(null);
	}

	public boolean isGuiOpen() {
		return currentGui != null;
	}

	public GuiScreen getCurrentGui() {
		return currentGui;
	}

	public double getPartialTick() {
		return partialTick;
	}

	public BlockRaycast getBlockRaycast() {
		return blockRaycast;
	}

	public static void main(String[] args) {
		new Game().run();
	}
}
