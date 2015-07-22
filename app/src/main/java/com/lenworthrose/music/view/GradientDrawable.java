package com.lenworthrose.music.view;

import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;

/**
 * The {@link PaintDrawable} used to draw the gradient behind {@link GridItem}'s label.
 */
public class GradientDrawable extends PaintDrawable {
    public GradientDrawable(int baseColor) {
        setShape(new RectShape());
        setShaderFactory(new ShaderFactory(baseColor));
    }

    private static class ShaderFactory extends ShapeDrawable.ShaderFactory {
        int baseColor;

        public ShaderFactory(int baseColor) {
            this.baseColor = baseColor;
        }

        @Override
        public Shader resize(int width, int height) {
            return new LinearGradient(0, 0, 0, height,
                    createGradientColorArray(baseColor),
                    new float[] { 0f, .023f, .039f, .056f, .080f, .110f, .165f, 1f },
                    Shader.TileMode.CLAMP);
        }

        private static int[] createGradientColorArray(int baseColor) {
            return new int[] { 0x00121212,
                    baseColor + (0x21 << 24),
                    baseColor + (0x3B << 24),
                    baseColor + (0x52 << 24),
                    baseColor + (0x70 << 24),
                    baseColor + (0x8A << 24),
                    baseColor + (0x9D << 24),
                    baseColor + (0xD0 << 24)};
        }
    }
}
