package chechelpo.frplm.frameworks.entities.assets;

import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.types.NotFound;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.utils.IO.FileReaders;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableRecord;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static chechelpo.frplm.utils.IO.FileWriters.writeImgToPath;


public abstract class AvatarManager <
        R extends TableRecord<R>,
        Reg extends EntityAssetRegistry<R, ?>
        > implements AssetManager<R>
{
    private final Reg registry;

    protected AvatarManager(@NotNull Reg assetRegistry) {
        this.registry = assetRegistry;
    }

    public @NotNull Optional<BufferedImage> getAvatar(@NotNull EntityKey<R> id) throws NotFound {
        Path path = registry.getPathToAsset(AssetType.AVATAR, id);
        return FileReaders.readImageFromPath(path);
    }

    public boolean setAvatar(@NotNull EntityKey<R> id, @NotNull MultipartFile avatar) throws IOException {
        Path path = registry.getPathToAsset(AssetType.AVATAR, id);
        writeImgToPath(path, avatar);
        return true;
    }

    @Override
    public @NotNull AssetType getType() {
        return AssetType.AVATAR;
    }

    @Override
    public @NotNull byte[] getAssetsOfEntity(@NotNull EntityKey<R> key, int number) throws IOException {
        Optional<BufferedImage> avatar = getAvatar(key);
        if (!avatar.isPresent())
            throw new NotFound("Avatar not found ", Severity.EXPECTED);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(avatar.get(), "png", stream);

        return stream.toByteArray();
    }

    @Override
    public boolean uploadAsset(@NotNull EntityKey<R> key, @NotNull MultipartFile asset, int number) throws IOException {
        return setAvatar(key, asset);
    }
}
