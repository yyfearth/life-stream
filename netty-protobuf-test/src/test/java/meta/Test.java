package meta;

import org.testng.annotations.*;

import java.util.UUID;

public class Test {
    private Meta.Image image;

    @BeforeClass
    public void setup() {
        image = Meta.Image.newBuilder()
                .setUuid(UUID.randomUUID().toString())
                .setFilename("test1.jpg")
                .setProcessed(false)
                .build();
    }

    @org.testng.annotations.Test(groups = {"fast"})
    public void aFastTest() {
        System.out.println("Fast test");
    }

    @org.testng.annotations.Test(groups = {"slow"})
    public void aSlowTest() {
        System.out.println("Slow test");
    }


}
