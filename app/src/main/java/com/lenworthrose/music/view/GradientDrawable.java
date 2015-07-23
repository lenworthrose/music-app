package com.lenworthrose.music.view;

import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;

/**
 * The {@link PaintDrawable} used to draw the gradient behind {@link GridItem}'s label, or
 * {@link com.lenworthrose.music.fragment.PlayingItemFragment}'s header or footer.
 */
public class GradientDrawable extends PaintDrawable {
    public enum Type {
        GRID_ITEM,
        PLAYING_NOW_HEADER,
        PLAYING_NOW_FOOTER
    }

    public GradientDrawable(Type type, int baseColor) {
        setShape(new RectShape());
        setShaderFactory(new ShaderFactory(type, baseColor));
    }

    private static class ShaderFactory extends ShapeDrawable.ShaderFactory {
        int baseColor;
        Type type;

        public ShaderFactory(Type type, int baseColor) {
            this.type = type;
            this.baseColor = baseColor;
        }

        @Override
        public Shader resize(int width, int height) {
            return new LinearGradient(0,
                    type == Type.PLAYING_NOW_HEADER ? height : 0,
                    0,
                    type == Type.PLAYING_NOW_HEADER ? 0 : height,
                    createGradientColorArray(type, baseColor),
                    createGradientPositionsArray(type),
                    Shader.TileMode.CLAMP);
        }

        private static int[] createGradientColorArray(Type type, int baseColor) {
            int[] alphas = createGradientAlphasArray(type);
            if (alphas == null) return null;
            int[] retVal = new int[alphas.length];
            baseColor &= (0xFFFFFF); //Clear alpha byte

            for (int i = 0; i < retVal.length; i++)
                retVal[i] = baseColor + (alphas[i] << 24);

            return retVal;
        }

        private static int[] createGradientAlphasArray(Type type) {
            switch (type) {
                case GRID_ITEM:
                    return new int[] { 0, 0x21, 0x3B, 0x52, 0x70, 0x8A, 0x9D, 0xD0 };
                case PLAYING_NOW_HEADER:
                    return new int[] { 0, 0x29, 0x49, 0x69, 0x7C, 0x8A, 0x9D, 0xB2 };
                case PLAYING_NOW_FOOTER:
                    return new int[] { 0, 0x4D, 0x69, 0x72, 0x76, 0x79, 0x87, 0x93, 0xA6, 0xB2 };
            }

            return null;
        }

        private static float[] createGradientPositionsArray(Type type) {
            switch (type) {
                case GRID_ITEM:
                    return new float[] { 0f, .023f, .039f, .056f, .080f, .110f, .165f, 1f };
                case PLAYING_NOW_HEADER:
                    return new float[] { 0f, .023f, .039f, .056f, .080f, .110f, .165f, 1f };
                case PLAYING_NOW_FOOTER:
                    return new float[] { 0f, .040f, .064f, .083f, .090f, .095f, .12f, .15f, .2f, 1f };
            }

            return null;
        }
    }
}
