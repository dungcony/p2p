package dungcony.ds.services.impl.security;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.security.RsaKeyPairUtil;
import dungcony.ds.services.interfaces.security.MessageEncryptionService;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
public class RsaMessageEncryptionService implements MessageEncryptionService {
    private static final int SHA256_BYTES = 32;
    private static final int OAEP_PADDING_BYTES = 2 * SHA256_BYTES + 2;
    private static final String CHUNK_SEPARATOR = ".";
    private static final String DECRYPTION_ERROR_TEXT = "[Unable to decrypt message]";

    private final PrivateKey privateKey;

    public RsaMessageEncryptionService(String privateKey) {
        this.privateKey = RsaKeyPairUtil.decodePrivateKey(privateKey);
    }

    @Override
    public Optional<Message> encryptForReceiver(Message message, PeerInfo receiver) {
        if (message == null || receiver == null) {
            return Optional.empty();
        }
        if (receiver.getPublicKey() == null || receiver.getPublicKey().isBlank()) {
            log.warn("Cannot encrypt message because receiver public key is missing. messageId={}, receiver={}",
                    message.getId(), receiver.getId());
            return Optional.empty();
        }
        try {
            PublicKey publicKey = RsaKeyPairUtil.decodePublicKey(receiver.getPublicKey());
            Message encryptedMessage = copyOf(message);
            encryptedMessage.setContent(encryptContent(message.getContent(), publicKey));
            encryptedMessage.setEncrypted(true);
            encryptedMessage.setEncryptionAlgorithm(RsaKeyPairUtil.MESSAGE_ALGORITHM);
            encryptedMessage.setEncryptedFor(receiver.getId());
            return Optional.of(encryptedMessage);
        } catch (RuntimeException e) {
            log.warn("Cannot encrypt message. messageId={}, receiver={}, cause={}",
                    message.getId(), receiver.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Message decrypt(Message message) {
        if (message == null || !message.isEncrypted()) {
            return message;
        }
        Message decryptedMessage = copyOf(message);
        try {
            decryptedMessage.setContent(decryptContent(message.getContent()));
            decryptedMessage.setEncrypted(false);
            return decryptedMessage;
        } catch (RuntimeException e) {
            log.warn("Cannot decrypt message. messageId={}, sender={}, cause={}",
                    message.getId(), message.getSenderId(), e.getMessage());
            decryptedMessage.setContent(DECRYPTION_ERROR_TEXT);
            decryptedMessage.setEncrypted(false);
            return decryptedMessage;
        }
    }

    private String encryptContent(String content, PublicKey publicKey) {
        byte[] plainBytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        int chunkSize = maxPlainChunkSize(publicKey);
        Cipher cipher = RsaKeyPairUtil.newCipher(Cipher.ENCRYPT_MODE, publicKey);
        List<String> chunks = new ArrayList<>();
        for (int offset = 0; offset < plainBytes.length; offset += chunkSize) {
            int length = Math.min(chunkSize, plainBytes.length - offset);
            try {
                chunks.add(Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(cipher.doFinal(plainBytes, offset, length)));
            } catch (Exception e) {
                throw new IllegalStateException("RSA encryption failed", e);
            }
        }
        return String.join(CHUNK_SEPARATOR, chunks);
    }

    private String decryptContent(String encryptedContent) {
        if (encryptedContent == null || encryptedContent.isBlank()) {
            return "";
        }
        Cipher cipher = RsaKeyPairUtil.newCipher(Cipher.DECRYPT_MODE, privateKey);
        ByteArrayOutputStream plainBytes = new ByteArrayOutputStream();
        for (String chunk : encryptedContent.split("\\.", -1)) {
            if (chunk.isBlank()) {
                continue;
            }
            try {
                byte[] encryptedChunk = Base64.getUrlDecoder().decode(chunk);
                plainBytes.write(cipher.doFinal(encryptedChunk));
            } catch (Exception e) {
                throw new IllegalStateException("RSA decryption failed", e);
            }
        }
        return plainBytes.toString(StandardCharsets.UTF_8);
    }

    private int maxPlainChunkSize(PublicKey publicKey) {
        if (publicKey instanceof RSAPublicKey rsaPublicKey) {
            return (rsaPublicKey.getModulus().bitLength() + 7) / 8 - OAEP_PADDING_BYTES;
        }
        return RsaKeyPairUtil.KEY_SIZE_BITS / 8 - OAEP_PADDING_BYTES;
    }

    private Message copyOf(Message source) {
        Message copy = new Message();
        copy.setId(source.getId());
        copy.setType(source.getType());
        copy.setSenderId(source.getSenderId());
        copy.setSenderHost(source.getSenderHost());
        copy.setSenderPort(source.getSenderPort());
        copy.setSenderPublicKey(source.getSenderPublicKey());
        copy.setReceiverId(source.getReceiverId());
        copy.setReceiverHost(source.getReceiverHost());
        copy.setReceiverPort(source.getReceiverPort());
        copy.setGroupId(source.getGroupId());
        copy.setGroupName(source.getGroupName());
        copy.setContent(source.getContent());
        copy.setTimestamp(source.getTimestamp());
        copy.setStatus(source.getStatus());
        copy.setPeers(source.getPeers());
        copy.setGroupMembers(source.getGroupMembers());
        copy.setFromCurrentUser(source.isFromCurrentUser());
        copy.setEncrypted(source.isEncrypted());
        copy.setEncryptionAlgorithm(source.getEncryptionAlgorithm());
        copy.setEncryptedFor(source.getEncryptedFor());
        return copy;
    }
}
