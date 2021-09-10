package org.riekr.jloga.misc;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public interface FileDropListener {

	void setFileDropListener(@NotNull Consumer<List<File>> consumer);

}
