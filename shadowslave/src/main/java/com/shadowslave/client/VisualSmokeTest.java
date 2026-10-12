package com.shadowslave.client;

import com.mojang.logging.LogUtils;
import com.shadowslave.ShadowSlaveMod;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

/** Opt-in CI check that draws the real Minecraft models and GLSL into an OpenGL framebuffer. */
@EventBusSubscriber(modid = ShadowSlaveMod.ID, value = Dist.CLIENT)
public final class VisualSmokeTest {
  private static final Logger LOG = LogUtils.getLogger();
  private static final boolean ENABLED = Boolean.getBoolean("shadowslave.visualSmoke");
  private static final String[] NAMES = {
    "species-overview", "forms-overview", "magic-overview", "forms-strike"
  };
  private static final int[] GROUPS = {0, 1, 2, 1};
  private static volatile boolean finished;
  private static long started;
  private static int readyTicks, settledTicks, stage;
  private static boolean displayConfigured;
  private static CreatureGalleryScreen gallery;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!ENABLED || finished) return;
    var mc = Minecraft.getInstance();
    if (started == 0) {
      started = System.nanoTime();
      LOG.info("VISUAL_SMOKE_START: native model atlas, built-in shader, four gallery captures");
      Thread watchdog = new Thread(() -> {
        try {
          Thread.sleep(120_000);
          if (!finished) mc.execute(() -> fail(mc, "120-second client watchdog expired", null));
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
      }, "shadowslave-visual-smoke-watchdog");
      watchdog.setDaemon(true);
      watchdog.start();
    }
    if (System.nanoTime() - started > 120_000_000_000L) {
      fail(mc, "120-second client timeout expired", null);
      return;
    }
    if (mc.getOverlay() != null) {
      readyTicks = 0;
      return;
    }
    if (++readyTicks < 5) return;
    if (!MagicShaders.isLoaded()) {
      fail(mc, "Bundled arcane GLSL shader did not load; fallback is not a shader pass", null);
      return;
    }
    try {
      if (!displayConfigured) {
        mc.options.guiScale().set(1);
        if (mc.getWindow().getWidth() < 1280 || mc.getWindow().getHeight() < 900)
          GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 900);
        mc.resizeDisplay();
        displayConfigured = true;
        readyTicks = 0;
        return;
      }
      if (gallery == null) {
        // Replaces title/accessibility onboarding only in the explicitly opted-in CI run.
        gallery = new CreatureGalleryScreen(mc.screen, GROUPS[stage], 0);
        gallery.setOverview(true);
        gallery.setAnimationMode(stage == 3 ? 3 : 0);
        mc.setScreen(gallery);
        // Ignore stale errors from startup; the next twenty frames belong to this stage.
        for (int error = 0; error < 16 && GL11.glGetError() != GL11.GL_NO_ERROR; error++) {}
        settledTicks = 0;
      } else if (mc.screen == gallery && gallery.hasRendered()) {
        settledTicks++;
      }
    } catch (RuntimeException | LinkageError error) {
      fail(mc, "Could not construct the real model gallery", error);
    }
  }

  @SubscribeEvent
  public static void rendered(ScreenEvent.Render.Post event) {
    if (!ENABLED || finished || gallery == null || event.getScreen() != gallery
        || !gallery.hasRendered() || settledTicks < 20) return;
    var mc = Minecraft.getInstance();
    try {
      event.getGuiGraphics().flush();
      int glError = GL11.glGetError();
      if (glError != GL11.GL_NO_ERROR)
        throw new IllegalStateException("Gallery draw produced OpenGL error 0x"
            + Integer.toHexString(glError));
      Path directory = Path.of(System.getProperty("shadowslave.visualSmokeDir",
          mc.gameDirectory.toPath().resolve("../build/reports/visual-smoke").normalize().toString()));
      Files.createDirectories(directory);
      Path output = directory.resolve(NAMES[stage] + ".png");
      try (var capture = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
        if (capture.getWidth() < 320 || capture.getHeight() < 240)
          throw new IllegalStateException("Framebuffer too small: "
              + capture.getWidth() + "x" + capture.getHeight());
        capture.writeToFile(output);
      }
      LOG.info("VISUAL_SMOKE_CAPTURE {} {}", NAMES[stage], output.toAbsolutePath());
      stage++;
      gallery = null;
      settledTicks = 0;
      if (stage == NAMES.length) {
        finished = true;
        LOG.info("VISUAL_SMOKE_PASS captures={} shader=arcane backend=OpenGL", stage);
        mc.stop();
      }
    } catch (Exception | LinkageError error) {
      fail(mc, "Could not draw or capture stage " + stage, error);
    }
  }

  private static void fail(Minecraft mc, String reason, Throwable error) {
    if (finished) return;
    finished = true;
    if (error == null) LOG.error("VISUAL_SMOKE_FAIL: {}", reason);
    else LOG.error("VISUAL_SMOKE_FAIL: " + reason, error);
    mc.stop();
  }
}
