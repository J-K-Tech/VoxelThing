package io.bluestaggo.voxelthing.gui.screen;

import io.bluestaggo.voxelthing.Game;
import io.bluestaggo.voxelthing.assets.FontManager;
import io.bluestaggo.voxelthing.renderer.screen.Screen;
import io.bluestaggo.voxelthing.world.block.Block;

public class DebugGui extends GuiScreen {
	public DebugGui(Game game) {
		super(game);
	}

	@Override
	public void draw() {
		super.draw();

		FontManager fonts = game.renderer.fonts;
		Screen screen = game.renderer.screen;

		fonts.outlined.print("§c00ffffVOXEL THING    §c00ff00" + Game.VERSION, 5, 5, 1.0f, 1.0f, 1.0f);

		long freeMB = Runtime.getRuntime().freeMemory() / 1000000L;
		long totalMB = Runtime.getRuntime().totalMemory() / 1000000L;
		long maxMB = Runtime.getRuntime().maxMemory() / 1000000L;

		String raycastText = Block.ID_AIR.toString();
		if (game.getBlockRaycast() != null) {
			raycastText = game.getBlockRaycast().getDebugText(game.world);
		}

		String[] lines = {
				"FPS", game.window.getFps() + " (" + (int)(game.window.getDeltaTime() * 1000.0D) + "ms)",
				"ticking entities:", " "+game.getTicking(),
				"Memory", ((totalMB - freeMB) * 100 / maxMB) + "% (" + (totalMB - freeMB) + " / " + maxMB + " MB)",
				"GUI Scale", String.valueOf(screen.scale <= 0.0f ? "auto" : screen.scale),
				"Position", game.isInWorld()
						? formatDouble(game.player.posX)
						+ ", " + formatDouble(game.player.posY)
						+ ", " + formatDouble(game.player.posZ)
						: "N/A",
				"Looking At", raycastText,
				"Scroll", formatDouble(game.window.getScrollX()) + ", " + formatDouble(game.window.getScrollY())
		};

		StringBuilder debugBuilder = new StringBuilder();

		for (int i = 0; i < lines.length / 2; i++) {
			String label = lines[i * 2];
			String value = lines[i * 2 + 1];

			if (!debugBuilder.isEmpty()) {
				debugBuilder.append('\n');
			}

			debugBuilder.append("§cffff7f");
			debugBuilder.append(label);
			debugBuilder.append(": §cffffff");
			debugBuilder.append(value);
		}

		fonts.shadowed.print(debugBuilder.toString(), 5, 15);
	}

	private static String formatDouble(double d) {
		return Double.toString(Math.floor(d * 100.0) / 100.0);
	}
}
