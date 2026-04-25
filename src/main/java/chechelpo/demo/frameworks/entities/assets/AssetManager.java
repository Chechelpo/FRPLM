package chechelpo.demo.frameworks.entities.assets;

import chechelpo.demo.frameworks.entities.data.EntityKey;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AssetManager <R extends TableRecord<R>> {
    @NotNull AssetType getType();
    @NotNull byte[] getAssetsOfEntity(@NotNull EntityKey<R> key, int number) throws IOException;
    boolean uploadAsset(@NotNull EntityKey<R> key, @NotNull MultipartFile asset, int number) throws IOException;
}
