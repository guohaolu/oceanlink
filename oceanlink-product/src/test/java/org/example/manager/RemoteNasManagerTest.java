package org.example.manager;

import org.example.manager.impl.RemoteNasManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class RemoteNasManagerTest {
    @Autowired
    private RemoteNasManager remoteNasManager;

    @Test
    void list() {
        List<String> list = remoteNasManager.list("/IT");
        System.out.println(list);
    }
}