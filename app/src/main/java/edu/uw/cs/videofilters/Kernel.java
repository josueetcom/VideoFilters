package edu.uw.cs.videofilters;

/**
 * Created by Josue Rios on 12/9/2017.
 */

class Kernel {
    private double[][] values;

    enum Type {
        BOTTOM_SOBEL,
        EMBOSS,
        GAUSSIAN_BLUR,
        IDENTITY,
        LEFT_SOBEL,
        MEAN_BLUR,
        OUTLINE,
        RIGHT_SOBEL,
        TOP_SOBEL,
        SHARPEN,
        CUSTOM
    }

    /** values[row][col] */
    Kernel(double[][] values) {
        if (BuildConfig.DEBUG && values.length % 2 != 1)
            throw new AssertionError();
        for (double[] value : values) {
            if (BuildConfig.DEBUG && value.length != values.length) {
                throw new AssertionError();
            }
        }
        this.values = values;
    }

    Kernel(Type kernelType) {
        values = new double[3][3];
        if (BuildConfig.DEBUG && kernelType == Type.CUSTOM)
            throw new AssertionError();
        switch (kernelType) {
            case BOTTOM_SOBEL:
            case TOP_SOBEL:
                values[kernelType == Type.TOP_SOBEL ? 0 : 2] = new double[]{ 1, 2, 1};
                values[kernelType == Type.TOP_SOBEL ? 2 : 0] = new double[]{ -1, -2, -1};
                break;
            case LEFT_SOBEL:
            case RIGHT_SOBEL:
                values[0][0] = values[2][0] = kernelType == Type.LEFT_SOBEL ? 1 : -1;
                values[1][0] = kernelType == Type.LEFT_SOBEL ? 2 : -2;

                values[0][2] = values[2][2] = kernelType == Type.LEFT_SOBEL ? -1 : 1;
                values[1][2] = kernelType == Type.LEFT_SOBEL ? -2 : 2;
                break;
            case IDENTITY:
                values[1][1] = 1;
                break;
            case GAUSSIAN_BLUR:
            case MEAN_BLUR:
            case OUTLINE:
                for (int i = 0; i < values.length; i++) {
                    for (int j = 0; j < values[i].length; j++) {
                        if (kernelType == Type.MEAN_BLUR) {
                            values[i][j] = 1.0 / 9;
                        } else if (kernelType == Type.OUTLINE) {
                            values[i][j] = i == 1 && j == 1 ? 8 : -1;
                        } else {
                            if (j == 1 && i == 1) {
                                values[i][j] = 0.25;
                            } else if ((j - i) % 2 == 0) {
                                values[i][j] = 0.0625;
                            } else {
                                values[i][j] = 0.125;
                            }
                        }
                    }
                }
                break;
            case SHARPEN:
                values[0][1] = values[-1][0] = values[-1][2] = values[2][1] = -1;
                values[1][1] = 5;
                break;
            case EMBOSS:
                values[0][0] = -2;
                values[0][1] = values[1][0] = -1;
                values[1][1] = values[1][2] = values[2][1] = 1;
                values[2][2] = 2;
                break;
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append('[');
        for (int i = 0; i < values.length; i++) {
            s.append('[');
            for (int j = 0; j < values[i].length; j++) {
                s.append(values[i][j]);
                if (j < values[j].length - 1) {
                    s.append(", ");
                }
            }
            s.append(']');
            if (i < values.length - 1) {
                s.append(", ");
            }
        }
        s.append(']');
        return s.toString();
    }
}
