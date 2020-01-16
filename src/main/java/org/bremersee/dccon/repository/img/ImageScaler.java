/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bremersee.dccon.repository.img;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * The image scaler.
 *
 * @author Christian Bremer
 */
public class ImageScaler {

  /**
   * Scale image.
   *
   * @param img the img
   * @param dimension the dimension
   * @return the buffered image
   */
  public static BufferedImage scaleImage(BufferedImage img, Dimension dimension) {
    img = scaleByHalf(img, dimension);
    img = scaleExact(img, dimension);
    return img;
  }

  private static BufferedImage scaleByHalf(BufferedImage img, Dimension dimension) {
    int w = img.getWidth();
    int h = img.getHeight();
    float factor = getBinFactor(w, h, dimension);

    // make new size
    w *= factor;
    h *= factor;
    BufferedImage scaled = new BufferedImage(w, h,
        BufferedImage.TYPE_INT_RGB);
    Graphics2D g = scaled.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g.drawImage(img, 0, 0, w, h, null);
    g.dispose();
    return scaled;
  }

  private static BufferedImage scaleExact(BufferedImage img, Dimension dimension) {
    float factor = getFactor(img.getWidth(), img.getHeight(), dimension);

    // create the image
    int w = (int) (img.getWidth() * factor);
    int h = (int) (img.getHeight() * factor);
    BufferedImage scaled = new BufferedImage(w, h,
        BufferedImage.TYPE_INT_RGB);

    Graphics2D g = scaled.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(img, 0, 0, w, h, null);
    g.dispose();
    return scaled;
  }

  private static float getBinFactor(int width, int height, Dimension dimension) {
    float factor = 1;
    float target = getFactor(width, height, dimension);
    if (target <= 1) {
      while (factor / 2 > target) {
        factor /= 2;
      }
    } else {
      while (factor * 2 < target) {
        factor *= 2;
      }
    }
    return factor;
  }

  private static float getFactor(int width, int height, Dimension dimension) {
    float sx = dimension.width / (float) width;
    float sy = dimension.height / (float) height;
    return Math.min(sx, sy);
  }
}