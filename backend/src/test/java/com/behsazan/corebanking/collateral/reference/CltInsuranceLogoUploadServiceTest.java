package com.behsazan.corebanking.collateral.reference;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CltInsuranceLogoUploadServiceTest {
    private static byte[] image(String format) throws Exception {
        BufferedImage source = new BufferedImage(64, 40, BufferedImage.TYPE_INT_RGB);
        try (var out = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(source, format, out));
            return out.toByteArray();
        }
    }

    @Test void pngIsAccepted() throws Exception {
        assertEquals("image/png", CltInsuranceLogoUploadService.checkedImage(image("png")));
    }
    @Test void jpegIsAccepted() throws Exception {
        assertEquals("image/jpeg", CltInsuranceLogoUploadService.checkedImage(image("jpg")));
    }
    @Test void gifIsAccepted() throws Exception {
        assertEquals("image/gif", CltInsuranceLogoUploadService.checkedImage(image("gif")));
    }
    @Test void svgIsRejected() {
        assertThrows(ResponseStatusException.class, () -> CltInsuranceLogoUploadService.checkedImage(
                "<svg><script>alert(1)</script></svg>".getBytes()));
    }
    @Test void htmlIsRejected() {
        assertThrows(ResponseStatusException.class, () -> CltInsuranceLogoUploadService.checkedImage(
                "<html><img src=x></html>".getBytes()));
    }
    @Test void oversizedUploadIsRejected() {
        assertThrows(ResponseStatusException.class, () -> CltInsuranceLogoUploadService.checkedImage(
                new byte[CltInsuranceImagePolicy.MAX_BYTES + 1]));
    }
    @Test void identifierAllowlist() {
        assertEquals(35, CltInsuranceLogoUploadService.checkedId("35"));
        for (String invalid : new String[] {"0", "-1", "1 OR 1=1", "", "1/2", "1.3"}) {
            assertThrows(ResponseStatusException.class, () -> CltInsuranceLogoUploadService.checkedId(invalid));
        }
    }
}
