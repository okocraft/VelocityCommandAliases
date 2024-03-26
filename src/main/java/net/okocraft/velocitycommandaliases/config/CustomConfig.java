package net.okocraft.velocitycommandaliases.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import net.okocraft.velocitycommandaliases.Main;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * Class for manipulating yaml files.
 *
 * @author LazyGon
 */
public abstract class CustomConfig {

    private final YamlConfigurationLoader loader;
    private final Main plugin;
    private final File file;
    private final String name;
    private ConfigurationNode config;

    CustomConfig(Main plugin, String name) {
        this.plugin = plugin;

        Path filePath = plugin.dataDirectory().resolve(name);

        this.loader = YamlConfigurationLoader.builder().path(filePath).build();
        this.name = name;
        this.file = filePath.toFile();
        reload();
        if (file.isDirectory()) {
            throw new IllegalArgumentException("file must not be directory");
        }
    }

    CustomConfig(Main plugin, File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("file must not be directory");
        }

        this.plugin = plugin;
        this.file = file;
        this.name = file.getName();
        this.loader = YamlConfigurationLoader.builder().path(file.toPath()).build();
        reload();
    }

    /**
     * Gets Configuration of {@code file}.
     *
     * @return Configuration
     * @author LazyGon
     */
    protected ConfigurationNode get() {
        if (config == null) {
            reload();
        }

        return config;
    }

    /**
     * Loads Configuration from {@code file}.
     *
     * @author LazyGon
     */
    public void reload() {
        saveDefault();
        try {
            config = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Cannot load configuration.", e);
        }

    }

    /**
     * Saves default file which is included in jar.
     *
     * @author LazyGon
     */
    public void saveDefault() {
        if (!file.exists()) {
            saveResource(name, false);
        }
    }

    private void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = plugin.getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + this.file);
        }

        File outFile = file;
        File outDir = file.getParentFile();
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        if (outFile.exists() && !replace) {
            plugin.logger().warn("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            return;
        }

        try {
            OutputStream out = new FileOutputStream(outFile);
            byte[] buf = new byte[1024];

            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            out.close();
            in.close();
            plugin.logger().info("file copied!");
        } catch (IOException e) {
            plugin.logger().error("Could not save " + outFile.getName() + " to " + outFile, e);
        }

    }

    /**
     * 設定ファイルを保存する。
     *
     * @author LazyGon
     */
    public void save() {
        if (config == null)
            return;
        try {
            loader.save(get());
        } catch (IOException e) {
            plugin.logger().error("Could not save config to " + file, e);
        }
    }
}