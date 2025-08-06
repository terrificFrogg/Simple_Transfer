package org.simpletransfer.models;

import java.util.List;

public interface Transfer {
    void startTransfer(List<ConfigGroups> configGroups);
    void stopTransfer();
}
