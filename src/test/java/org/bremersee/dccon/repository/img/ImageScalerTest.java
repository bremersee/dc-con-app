/*
 * Copyright 2019-2020 the original author or authors.
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
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * The image scaler test.
 *
 * @author Christian Bremer
 */
@ExtendWith(SoftAssertionsExtension.class)
class ImageScalerTest {

  private static final String IMAGE_LOCATION = "classpath:mp.jpg";

  private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

  /**
   * Scale to smaller image.
   *
   * @param softly the soft assertions
   * @throws IOException the io exception
   */
  @Test
  void scaleToSmallerImage(SoftAssertions softly) throws IOException {
    BufferedImage original = ImageIO
        .read(RESOURCE_LOADER.getResource(IMAGE_LOCATION).getInputStream());
    BufferedImage actual = ImageUtils.scaleImage(original, new Dimension(20, 20));
    softly.assertThat(actual)
        .extracting(BufferedImage::getHeight, InstanceOfAssertFactories.INTEGER)
        .isLessThan(original.getHeight());
    softly.assertThat(actual)
        .extracting(BufferedImage::getWidth, InstanceOfAssertFactories.INTEGER)
        .isLessThan(original.getWidth());
  }

  /**
   * Scale to bigger image.
   *
   * @param softly the soft assertions
   * @throws IOException the io exception
   */
  @Test
  void scaleToBiggerImage(SoftAssertions softly) throws IOException {
    BufferedImage original = ImageIO
        .read(RESOURCE_LOADER.getResource(IMAGE_LOCATION).getInputStream());
    BufferedImage actual = ImageUtils.scaleImage(original, new Dimension(5000, 5000));
    softly.assertThat(actual)
        .extracting(BufferedImage::getHeight, InstanceOfAssertFactories.INTEGER)
        .isGreaterThan(original.getHeight());
    softly.assertThat(actual)
        .extracting(BufferedImage::getWidth, InstanceOfAssertFactories.INTEGER)
        .isGreaterThan(original.getWidth());
  }
}