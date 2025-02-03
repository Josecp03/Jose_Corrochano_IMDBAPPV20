package edu.pmdm.corrochano_josimdbapp;

import java.util.ArrayList;
import java.util.List;

public class RapidApiKeyManager {

    private List<String> apiKeys = new ArrayList<>();
    private int currentIndex = 0;

    public RapidApiKeyManager() {

        //apiKeys.add("357e3a5e4amsh7fabcc0ad2d98c2p1d0d29jsn1d6f8ef05aa7");
        apiKeys.add("eb2e298799mshf5bb21bd8905c9ap1c1723jsn5f7145733ca9");
        apiKeys.add("1d6b8bf5bemsh13bad6e5b669b95p146504jsnaa743711d880");
        apiKeys.add("440d48ca01mshb9178145c398148p1c905ajsn498799d4ab35");

    }

    public String getCurrentKey() {
        if (!apiKeys.isEmpty()) {
            return apiKeys.get(currentIndex);
        }
        return null;
    }

    public void switchToNextKey() {
        if (!apiKeys.isEmpty()) {
            currentIndex = (currentIndex + 1) % apiKeys.size();
        }
    }

}
