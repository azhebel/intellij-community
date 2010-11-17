package org.jetbrains.plugins.groovy.dsl.toplevel

import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.ElementPattern
import org.jetbrains.plugins.groovy.dsl.toplevel.scopes.Scope
import static com.intellij.patterns.PlatformPatterns.psiFile
import static com.intellij.patterns.PlatformPatterns.virtualFile
import org.jetbrains.plugins.groovy.dsl.GroovyClassDescriptor
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.extensions.GroovyScriptTypeDetector

/**
 * @author ilyas
 */
class Context {
  private List<ContextFilter> myFilters = []

  public Context(Map args) {
    // Named parameter processing
    if (!args) return

    // filetypes : [<file_ext>*]
    List<String> extensions = args.filetypes
    if (extensions instanceof List) {
      extensions = extensions.collect { StringUtil.trimStart(it, '.') }
      def vfilePattern = extensions.size() == 1 ? virtualFile().withExtension(extensions[0]) : virtualFile().withExtension(extensions as String[])
      addFilter new FileContextFilter(psiFile().withVirtualFile(vfilePattern))
    }

    List<String> scripttype = args.scripttype
    if (scripttype) {
      addFilter(new ContextFilter() {
                @Override
                boolean isApplicable(GroovyClassDescriptor descriptor, ProcessingContext ctx) {
                  def file = descriptor.placeFile
                  if (file instanceof GroovyFile && ((GroovyFile)file).isScript()) {
                    def scripTypeId = GroovyScriptTypeDetector.getScriptType((GroovyFile)file).getId()
                    return scripttype.contains(scripTypeId)
                  }
                  return false
                }
                })
    }

    // filter by scope first, then by ctype
    // scope: <scope>
    if (args.scope) {
      myFilters += ((Scope) args.scope).createFilters(args)
    }

    // ctype : <ctype>
    // Qualifier type to be augmented
    if (args.ctype instanceof String) {
      addFilter ClassContextFilter.subtypeOf(args.ctype)
    }
    else if (args.ctype instanceof ElementPattern) {
      addFilter ClassContextFilter.fromClassPattern(args.ctype)
    }
  }

  private void addFilter(ContextFilter cl) {
    myFilters << cl
  }

  ContextFilter getFilter() {
    if (myFilters.size() == 1) {
      return myFilters[0]
    }

    return CompositeContextFilter.compose(myFilters, true)
  }

}
