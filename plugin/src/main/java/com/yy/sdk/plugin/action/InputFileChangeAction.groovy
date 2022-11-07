package com.yy.sdk.plugin.action

import org.gradle.api.Action
import org.gradle.api.tasks.incremental.InputFileDetails;

/**
 * Created by nls on 2022/8/6
 * Description: InputFileChangeAction
 */
public class InputFileChangeAction implements Action<InputFileDetails> {

    private String suffix
    private List<File> changes = new ArrayList<>()
    private List<File> removes = new ArrayList<>()
    private List<File> adds = new ArrayList<>()

    public InputFileChangeAction(String suffix) {
        this.suffix = suffix
    }

    @Override
    void execute(InputFileDetails inputFileDetails) {
        File file = inputFileDetails.getFile()
        if (inputFileDetails.added) {
            adds.add(file)
        } else if (inputFileDetails.removed) {
            removes.add(file)
        } else if (inputFileDetails.modified) {
            changes.add(file)
        }
    }

    List<File> getAllChanged() {
        List<File> allChanges = new ArrayList<>()
        if (!changes.isEmpty()) {
            allChanges.addAll(changes)
        }
        if (!removes.isEmpty()) {
            allChanges.addAll(removes)
        }
        if (!adds.isEmpty()) {
            allChanges.addAll(adds)
        }
        return allChanges
    }

    List<File> getChanges() {
        return changes
    }

    List<File> getRemoves() {
        return removes
    }

    List<File> getAdds() {
        return adds
    }
}
