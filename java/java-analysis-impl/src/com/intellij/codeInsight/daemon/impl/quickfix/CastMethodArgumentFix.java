/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.codeInsight.daemon.impl.quickfix;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInsight.daemon.impl.analysis.JavaHighlightUtil;
import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CastMethodArgumentFix extends MethodArgumentFix implements HighPriorityAction {
  private final String myText;

  private CastMethodArgumentFix(PsiExpressionList list, int i, PsiType toType, final ArgumentFixerActionFactory factory) {
    super(list, i, toType, factory);
    myText = myArgList.getExpressionCount() == 1
             ? QuickFixBundle.message("cast.single.parameter.text", JavaHighlightUtil.formatType(myToType))
             : QuickFixBundle.message("cast.parameter.text", myIndex + 1, JavaHighlightUtil.formatType(myToType));
  }

  @Override
  @NotNull
  public String getText() {
    return myText;
  }

  private static class MyFixerActionFactory extends ArgumentFixerActionFactory {
    @Override
    public CastMethodArgumentFix createFix(final PsiExpressionList list, final int i, final PsiType toType) {
      return new CastMethodArgumentFix(list, i, toType, this);
    }

    @Override
    protected PsiExpression getModifiedArgument(final PsiExpression expression, PsiType toType) throws IncorrectOperationException {
      final PsiType exprType = expression.getType();
      if (exprType instanceof PsiClassType && toType instanceof PsiPrimitiveType) {
        toType = ((PsiPrimitiveType)toType).getBoxedType(expression);
        assert toType != null;
      }
      return AddTypeCastFix.createCastExpression(expression, expression.getProject(), toType);
    }

    @Override
    public boolean areTypesConvertible(@NotNull PsiType exprType, @NotNull PsiType parameterType, @NotNull final PsiElement context) {
      if (exprType instanceof PsiClassType && parameterType instanceof PsiPrimitiveType) {
        parameterType = ((PsiPrimitiveType)parameterType).getBoxedType(context); //unboxing from type of cast expression will take place at runtime
        if (parameterType == null) return false;
      }
      if (exprType instanceof PsiPrimitiveType && parameterType instanceof PsiClassType) {
        if (PsiType.NULL.equals(exprType)) {
          return true;
        }
        parameterType = PsiPrimitiveType.getUnboxedType(parameterType);
        if (parameterType == null) return false;
      }
      if (parameterType.isConvertibleFrom(exprType)) {
        return true;
      }

      return parameterType instanceof PsiEllipsisType &&
             areTypesConvertible(exprType, ((PsiEllipsisType)parameterType).getComponentType(), context);
    }
  }

  @Override
  public @Nullable FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
    return new CastMethodArgumentFix(CodeInsightUtilCore.findSameElementInCopy(myArgList, target), myIndex, myToType,
                                     myArgumentFixerActionFactory);
  }

  public static final ArgumentFixerActionFactory REGISTRAR = new MyFixerActionFactory();
}
