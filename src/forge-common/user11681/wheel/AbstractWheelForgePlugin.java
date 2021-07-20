package user11681.wheel;

import java.util.Objects;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Node;
import user11681.uncheck.Uncheck;
import user11681.wheel.extension.WheelExtension;
import user11681.wheel.extension.WheelForgeExtensionBase;

public abstract class AbstractWheelForgePlugin<P extends AbstractWheelForgePlugin<P, E>, E extends WheelExtension & WheelForgeExtensionBase> extends WheelPlugin<P, E> {
    protected static final String FORGE_METADATA_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml";

    protected static Node versioning() {
        return Uncheck.handle(() -> DocumentBuilderFactory
            .newDefaultInstance()
            .newDocumentBuilder()
            .parse(FORGE_METADATA_URL)
            .getFirstChild()
            .getLastChild()
        );
    }

    protected static Stream<String> nodeStream(Node first) {
        return Stream.iterate(first, Objects::nonNull, Node::getNextSibling).map(Node::getTextContent);
    }

    @Override
    public String metadataFile() {
        return "mods.toml";
    }

    @Override
    public void checkMinecraftVersion() {
        Node versioning = null;

        if (this.extension.minecraft == null) {
            versioning = versioning();

            if (latestMinecraftVersion == null) {
                if (this.extension.forge() == null) {
                    String[] versions = versioning.getChildNodes().item(1).getTextContent().split("-", 2);

                    this.extension.minecraft = latestMinecraftVersion = versions[0];
                    this.extension.forge(versions[1]);
                } else {
                    this.extension.minecraft = nodeStream(versioning.getLastChild().getFirstChild())
                        .filter(version -> version.endsWith(this.extension.forge()))
                        .findFirst()
                        .map(version -> version.substring(0, version.indexOf('-')))
                        .orElseThrow(() -> new IllegalArgumentException("Forge version \"%s\" was not found at %s.".formatted(this.extension.forge(), FORGE_METADATA_URL)));
                }
            } else {
                this.extension.minecraft = latestMinecraftVersion;
            }
        }

        if (this.extension.forge() == null) {
            this.extension.forge(nodeStream((versioning == null ? versioning() : versioning).getLastChild().getFirstChild())
                .filter(version -> version.startsWith(this.extension.minecraft))
                .findFirst()
                .map(version -> version.substring(version.indexOf('-') + 1))
                .orElseThrow(() -> new IllegalArgumentException("Minecraft version \"%s\" was not found at %s.".formatted(this.extension.minecraft, FORGE_METADATA_URL)))
            );
        }

        this.log("Forge version: {}", this.extension.forge());
    }

    @Override
    public String defaultJavaVersion() {
        return Integer.parseInt(this.extension.minecraft.split("\\.", 3)[1]) >= 17 ? "16" : "8";
    }
}
