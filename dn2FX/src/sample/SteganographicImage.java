package sample;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Steganographic image is an image that encodes hidden data. This class enables encoding / decoding a
 * text message followed by optional binary files. This behavior is similar to composing an e-mail,
 * except that the final result is encoded into an image.
 * <pre>
 * {@code
 * // Example of encoding.
 * SteganographicImage steganographicImage =
 *     SteganographicImage.loadFromFile("example.jpg");
 * steganographicImage.setMessage("This is the hidden message.");
 * steganographicImage.addAttachment("file1.doc");
 * steganographicImage.addAttachment("file2.txt");
 * steganographicImage.saveToFile("example_encoded.png");
 * System.out.println("Remaining storage: " +
 *     Integer.toString(stenographicImage.getTotalStorage() - stenographicImage.getUsedStorage()));
 *
 * // Example of decoding.
 * System.out.println(steganographicImage.getMessage());
 * String[] attachments = steganographicImage.listAttachments();
 * System.out.println("Attachments:");
 * for (int i = 0; i < attachments.length; i++) {
 *     System.out.println(attachments[i]);
 *     steganographicImage.saveAttachment(attachments[i]);
 * }
 * }
 * </pre>
 *
 * Encoded data structure:
 * <pre>
 *   signature "steganography" (13 bytes)
 *   message size              (32-bit integer)
 *   message                   (bytes)
 *   filename 1                (bytes)
 *   0x0                       (1 byte)
 *   file 1 size               (32-bit integer)
 *   file 1 (bytes)            (bytes)
 *   ...
 *   filename n                (bytes)
 *   0x0                       (1 byte)
 *   file n size               (32-bit integer)
 *   file n (bytes)            (bytes)
 *   0x0                       (1 byte)
 * </pre>
 *
 * @author Domen Šoberl @ UL FRI
 * @author Jure Žabkar  @ UL FRI
 * @version 2/2018
 */
public class SteganographicImage extends BufferedImage {

    public class SteganographicImageException extends Exception {
        SteganographicImageException(String message) {
            super(message);
        }
    }

    protected class Pipeline {
        private int[] data;
        private int pixel;
        private int component;

        public Pipeline(int[] data) {
            this.data = data;
            pixel = 0;
            component = 0;
        }

        public boolean eof() {
            return pixel >= data.length;
        }

        private int readBit() {
            if (eof()) return 0;

            int value = 0;
            switch (component) {
                case 0: // red
                    value = (data[pixel] & 0x00010000) == 0 ? 0 : 1;
                    component = 1;
                    break;
                case 1: // green
                    value = (data[pixel] & 0x00000100) == 0 ? 0 : 1;
                    component = 2;
                    break;
                case 2: // blue
                    value = (data[pixel] & 0x00000001) == 0 ? 0 : 1;
                    component = 0;
                    pixel++;
                    break;
            }

            return value;
        }

        private void writeBit(int bit) {
            if (eof()) return;

            switch (component) {
                case 0: // red
                    if (bit == 0)
                        data[pixel] = data[pixel] & 0xfffeffff;
                    else
                        data[pixel] = data[pixel] | 0x00010000;
                    component = 1;
                    break;
                case 1: // green
                    if (bit == 0)
                        data[pixel] = data[pixel] & 0xfffffeff;
                    else
                        data[pixel] = data[pixel] | 0x00000100;
                    component = 2;
                    break;
                case 2: // blue
                    if (bit == 0)
                        data[pixel] = data[pixel] & 0xfffffffe;
                    else
                        data[pixel] = data[pixel] | 0x00000001;
                    component = 0;
                    pixel++;
                    break;
            }
        }

        public int bytesLeft() {
            int pixels = data.length - pixel;
            int availableBits = pixels * 3;
            availableBits -= component;
            int availableBytes = availableBits / 8;
            return availableBytes;
        }

        public boolean writeByte(byte value) {
            if (bytesLeft() < 1) return false;

            int bits = (int)value << 24;
            for (int i = 0; i < 8; i++) {
                int bit = ((bits & 0x80000000) == 0 ? 0 : 1);
                bits = bits << 1;
                writeBit(bit);
            }
            return true;
        }

        public boolean writeInt(int value) {
            if (bytesLeft() < 4) return false;

            int bits = value;
            for (int i = 0; i < 32; i++) {
                int bit = ((bits & 0x80000000) == 0 ? 0 : 1);
                bits = bits << 1;
                writeBit(bit);
            }
            return true;
        }

        public boolean writeBytes(byte[] bytes) {
            if (bytesLeft() < bytes.length) return false;
            for (int i = 0; i < bytes.length; i++)
                writeByte(bytes[i]);
            return true;
        }

        public boolean writeString(String text) {
            byte[] bytes = text.getBytes();
            if (bytesLeft() < bytes.length + 1) return false;
            writeBytes(bytes);
            writeByte((byte)0);
            return true;
        }

        public byte readByte() {
            int value = 0;
            for (int i = 0; i < 8 && !eof(); i++) {
                value = value << 1;
                value = value | readBit();
            }
            return (byte)value;
        }

        public int readInt() {
            int value = 0;
            for (int i = 0; i < 32 && !eof(); i++) {
                value = value << 1;
                value = value | readBit();
            }
            return value;
        }

        public byte[] readBytes(int count) {
            if (count < 0) count = 0;
            byte[] bytes = new byte[count];
            for (int i = 0; i < count && !eof(); i++) {
                bytes[i] = readByte();
            }
            return bytes;
        }

        public String readString() {
            String text = "";
            boolean terminate = false;
            while (!terminate && !eof()) {
                byte value = readByte();
                if (value != 0)
                    text += (char)value;
                else
                    terminate = true;
            }
            return text;
        }

        public void clearToEnd() {
            while (!eof()) {
                writeBit(0);
            }
        }

        public void skip(int bytes) {
            int skipBits = bytes * 8;
            pixel += skipBits / 3;
            component += skipBits % 3;
            if (component > 2) {
                component -= 3;
                pixel++;
            }
        }
    }

    private static byte[] signature = {'s', 't', 'e', 'g', 'a', 'n', 'o', 'g', 'r', 'a', 'p', 'h', 'y'};
    private static int attachmentSizeLimit = 1024 * 1024 * 1024; // 1 GB

    /**
     * Creates a new SteganographicImage object from image stored in a file.
     *
     * @param  filename    Location of the file.
     * @return             The newly created stenographic image.
     * @throws IOException If error while reading the file.
     */
    public static SteganographicImage loadFromFile(String filename) throws IOException {
        Image image = ImageIO.read(new File(filename));
        return new SteganographicImage(image);
    }

    /**
     * Creates a new SteganographicImage object from a web image.
     *
     * @param  url         Location of the image.
     * @return             The newly created stenographic image.
     * @throws IOException If error accessing URL.
     */
    public static SteganographicImage loadFromUrl(String url) throws IOException {
        URL l = new URL(url);
        Image image = ImageIO.read(new URL(url));
        return new SteganographicImage(image);
    }

    /**
     * New stenographic image from Image.
     *
     * @param image Original image. A copy of this image will be used to store
     *              messages and attachments.
     */
    public SteganographicImage(Image image) {
        super(image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = createGraphics();
        graphics2D.drawImage(image, 0, 0, null);
        graphics2D.dispose();
    }

    /**
     * Returns the total available storage. This depends on the size of the image.
     * Each pixel stores exactly 3 bits. Alpha channel is never modified.
     *
     * @return Total storage in bytes.
     */
    public int getTotalStorage() {
        return getWidth() * getHeight() * 3 / 8;
    }

    /**
     * Returns the amount of currently used storage by message and attachments.
     * Note that even a cleared image uses a few bytes to store the signature.
     *
     * @return Used storage in bytes.
     */
    public int getUsedStorage() {
        DataBufferInt dataBufferInt = (DataBufferInt)getRaster().getDataBuffer();
        Pipeline pipeline = new Pipeline(dataBufferInt.getData());
        int count = 0;

        /* Read signature. */
        if (Arrays.equals(pipeline.readBytes(signature.length), signature))
            count += signature.length;
        else
            return 0;

        /* Message size. */
        int msgSize = pipeline.readInt();
        count += msgSize + 4;

        /* Message. */
        pipeline.skip(msgSize);

        /* Attachments. */
        boolean endOfList = false;
        while (!endOfList && !pipeline.eof()) {
            String filename = pipeline.readString();
            if (filename.length() > 0) {
                count += filename.length();
                int fileSize = pipeline.readInt();
                count += 4 + fileSize;
                pipeline.skip(fileSize);
            }
            else {
                endOfList = true;
            }
        }

        return count;
    }

    /**
     * Returns the file size limit for attachments. The number of attached files
     * is limited only by the available storage.
     *
     * @return File size limit for attached files.
     */
    public int getAttachmentSizeLimit() {
        return attachmentSizeLimit;
    }

    /**
     * Removes messages and attachments. Also writes the signature into the image.
     */
    public void clear() {
        DataBufferInt dataBufferInt = (DataBufferInt)getRaster().getDataBuffer();
        Pipeline pipeline = new Pipeline(dataBufferInt.getData());
        pipeline.writeBytes(signature);
        pipeline.clearToEnd();
    }

    /**
     * Encodes a text messages into the image. If a message already exists, it is overwritten.
     * Any existing attachments are removed from the image.
     *
     * @param  message                    Text message to be encoded.
     * @throws SteganographicImageException If not enough storage.
     */
    public void setMessage(String message) throws SteganographicImageException {
        DataBufferInt dataBufferInt = (DataBufferInt)getRaster().getDataBuffer();
        Pipeline pipeline = new Pipeline(dataBufferInt.getData());

        /* Write signature. */
        if (!pipeline.writeBytes(signature))
            throw new SteganographicImageException("Not enough storage to set the signature.");

        /* Write message. */
        boolean success = pipeline.writeInt(message.getBytes().length);
        if (success)
            success = pipeline.writeString(message);
        pipeline.clearToEnd();
        if (!success)
            throw new SteganographicImageException("Not enough storage to set the message.");
    }

    /**
     * Returns the stored message. If there is no message, an empty string is returned.
     *
     * @return Stored message.
     */
    public String getMessage() {
        DataBufferInt dataBufferInt = (DataBufferInt)getRaster().getDataBuffer();
        Pipeline pipeline = new Pipeline(dataBufferInt.getData());

        /* Check signature. */
        if (!Arrays.equals(pipeline.readBytes(signature.length), signature) || pipeline.eof())
            return "";

        /* Read message size. */
        int msgSize = pipeline.readInt();
        if (pipeline.eof()) return "";

        /* Read message. */
        byte[] data = pipeline.readBytes(msgSize);
        String message = new String(data);

        return message;
    }

    /**
     * Encodes a file into the image. The file is attached after the message. If there are
     * other attachments, the file is attached at the end. Attachments without the message
     * are possible.
     *
     * @param  filename                   The name of the file to be attached.
     * @throws IOException                If error while reading the file.
     * @throws SteganographicImageException If not enough storage.
     */
    public void addAttachment(String filename) throws IOException, SteganographicImageException {
        File file = new File(filename);
        if (file.length() > attachmentSizeLimit)
            throw new SteganographicImageException("Attachments are limited to " +
                    Integer.toString(attachmentSizeLimit) + " bytes.");

        String attachmentName = file.getName();
        int fileSize = (int)file.length();

        /* Check if the attachment already exists. */
        String[] attachments = listAttachments();
        for (int i = 0; i < attachments.length; i++) {
            if (attachments[i].equals(attachmentName))
                throw new SteganographicImageException("Attachment " + attachmentName + " already exists.");
        }

        /* Read the file. */
        byte[] data = new byte[fileSize];
        FileInputStream is = new FileInputStream(file);
        is.read(data);
        is.close();

        /* Process image. */
        DataBufferInt dataBufferInt = (DataBufferInt)getRaster().getDataBuffer();
        Pipeline pipeline = new Pipeline(dataBufferInt.getData());

        /* Check the signature. */
        if (!Arrays.equals(pipeline.readBytes(signature.length), signature))
            clear();

        /* Skip the message. */
        int msgSize = pipeline.readInt();
        pipeline.skip(msgSize);

        /* Skip all attachments. */
        for (int i = 0; i < attachments.length; i++) {
            pipeline.skip(attachments[i].length() + 1);
            int attachmentSize = pipeline.readInt();
            pipeline.skip(attachmentSize);
        }

        /* Check if enough storage is left. */
        int attachmentSize = attachmentName.getBytes().length + fileSize + 5;
        if (attachmentSize > pipeline.bytesLeft())
            throw new SteganographicImageException("Not enough storage to add the attachment.");

        /* Add the attachment. */
        pipeline.writeString(attachmentName);
        pipeline.writeInt(fileSize);
        pipeline.writeBytes(data);
    }

    /**
     * Saves an attachment to a file with the same name.
     *
     * @param  attachment                 The name of the attachment to be saved.
     * @throws IOException                If error while saving the file.
     * @throws SteganographicImageException If the given attachment does not exist.
     */
    public void saveAttachment(String attachment) throws IOException, SteganographicImageException {
        saveAttachment(attachment, attachment);
    }

    /**
     * Saves an attachment to a file with the given name.
     *
     * @param  attachment                 The name of the attachment to be saved.
     * @param  filename                   The name of the file to save the attachment to.
     * @throws IOException                If error while saving the file.
     * @throws SteganographicImageException If the given attachment does not exist.
     */
    public void saveAttachment(String attachment, String filename) throws IOException, SteganographicImageException {
        DataBufferInt dataBufferInt = (DataBufferInt)getRaster().getDataBuffer();
        Pipeline pipeline = new Pipeline(dataBufferInt.getData());

        /* The sole stenographic exception this method may throw. */
        SteganographicImageException attachmentException =
                new SteganographicImageException("Attachment " + attachment + " does not exist.");

        /* Check signature. */
        if (!Arrays.equals(pipeline.readBytes(signature.length), signature) || pipeline.eof())
            throw attachmentException;

        /* Read message size. */
        int msgSize = pipeline.readInt();
        if (pipeline.eof())  throw attachmentException;

        /* Skip message .*/
        pipeline.skip(msgSize);
        if (pipeline.eof()) throw attachmentException;

        /* Find the attachments. */
        boolean endOfList = false;
        boolean attachmentFound = false;
        while (!attachmentFound && !endOfList && !pipeline.eof()) {
            String attachmentName = pipeline.readString();
            if (attachmentName.length() > 0) {
                if (attachmentName.equals(attachment)) {
                    attachmentFound = true;
                }
                else {
                    int fileSize = pipeline.readInt();
                    pipeline.skip(fileSize);
                }
            }
            else
                endOfList = true;
        }

        if (!attachmentFound)
            throw attachmentException;

        /* Read the attachment. */
        int fileSize = pipeline.readInt();
        byte[] data = pipeline.readBytes(fileSize);

        /* Save the attachment. */
        FileOutputStream os = new FileOutputStream(new File(filename));
        os.write(data);
        os.close();
    }

    /**
     * Returns the list of all stored attachments. The name of the attachment is usually
     * the name of the file. If there are no attachments, an empty array is returned.
     *
     * @return Array of file names.
     */
    public String[] listAttachments() {
        DataBufferInt dataBufferInt = (DataBufferInt)getRaster().getDataBuffer();
        Pipeline pipeline = new Pipeline(dataBufferInt.getData());

        /* Check signature. */
        if (!Arrays.equals(pipeline.readBytes(signature.length), signature) || pipeline.eof())
            return new String[0];

        /* Read message size. */
        int msgSize = pipeline.readInt();
        if (pipeline.eof()) return new String[0];

        /* Skip message .*/
        pipeline.skip(msgSize);
        if (pipeline.eof()) return new String[0];

        /* List file names. */
        ArrayList<String> attachments = new ArrayList<>();
        boolean endOfList = false;
        while (!endOfList && !pipeline.eof()) {
            String filename = pipeline.readString();
            if (filename.length() > 0) {
                attachments.add(filename);
                int fileSize = pipeline.readInt();
                pipeline.skip(fileSize);
            }
            else
                endOfList = true;
        }

        return attachments.toArray(new String[attachments.size()]);
    }

    /**
     * Saves the image to a file. The image is saved in PNG format. Saving to a JPEG or any
     * other lossy format, the encoded data can get corrupted.
     *
     * @param  filename    The name of the file to save the image.
     * @throws IOException If error while writing to file.
     */
    public void saveToFile(String filename) throws IOException {
        ImageIO.write(this, "png", new File(filename));
    }
}
