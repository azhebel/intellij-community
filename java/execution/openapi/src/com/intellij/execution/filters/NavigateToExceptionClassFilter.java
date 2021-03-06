// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.filters;

import com.intellij.execution.filters.Filter.ResultItem;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NavigateToExceptionClassFilter implements JvmExceptionOccurrenceFilter {
  @Override
  public @Nullable ResultItem applyFilter(@NotNull String exceptionClassName,
                                                 @NotNull List<PsiClass> classes,
                                                 int exceptionStartOffset) {
    PsiClass psiClass = classes.get(0);
    boolean inContent =
      ProjectRootManager.getInstance(psiClass.getProject()).getFileIndex().isInContent(psiClass.getContainingFile().getVirtualFile());
    TextAttributes attributes = ExceptionInfoCache.ClassResolveInfo.getLinkAttributes(!inContent);
    HyperlinkInfo hyperlink = HyperlinkInfoFactory.getInstance().createMultiplePsiElementHyperlinkInfo(classes);
    return new Filter.Result(exceptionStartOffset, exceptionStartOffset + exceptionClassName.length(), 
                             hyperlink, attributes, attributes);
  }
}
