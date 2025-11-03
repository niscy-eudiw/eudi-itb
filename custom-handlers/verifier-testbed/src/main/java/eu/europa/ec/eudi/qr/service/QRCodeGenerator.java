/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.europa.ec.eudi.qr.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class QRCodeGenerator {

  /**
   * Generates a PNG image of a QR code as a byte array for the provided data.
   *
   * @param data The content to encode in the QR code.
   * @param width The image width (pixels).
   * @param height The image height (pixels).
   * @return PNG bytes of the generated QR code.
   */
  public static byte[] generate(String data, int width, int height)
      throws WriterException, IOException {
    if (data == null) {
      data = "";
    }
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Width and height must be positive");
    }

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
    hints.put(EncodeHintType.MARGIN, 1);

    QRCodeWriter writer = new QRCodeWriter();
    BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

    int matrixWidth = matrix.getWidth();
    int matrixHeight = matrix.getHeight();
    BufferedImage image = new BufferedImage(matrixWidth, matrixHeight, BufferedImage.TYPE_INT_RGB);
    int onColor = Color.BLACK.getRGB();
    int offColor = Color.WHITE.getRGB();
    for (int y = 0; y < matrixHeight; y++) {
      for (int x = 0; x < matrixWidth; x++) {
        image.setRGB(x, y, matrix.get(x, y) ? onColor : offColor);
      }
    }

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(image, "PNG", baos);
      return baos.toByteArray();
    }
  }
}
