/*
 * Copyright 2014-2018 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import groovy.lang.GroovyClassLoader;

/**
 * General scripting utility methods used by TNT4J-Streams.
 * 
 * @version $Revision: 2 $
 */
public final class StreamsScriptingUtils {
	/**
	 * Constant for field value variable name used in script/expression code.
	 */
	public static final String FIELD_VALUE_VARIABLE_NAME = "fieldValue"; // NON-NLS
	/**
	 * Constant for field value variable expression used in script/expression code.
	 */
	public static final String FIELD_VALUE_VARIABLE_EXPR = '$' + FIELD_VALUE_VARIABLE_NAME;

	/**
	 * Constant for name of scripting/expression language {@value}.
	 */
	public static final String GROOVY_LANG = "groovy"; // NON-NLS
	/**
	 * Constant for name of scripting/expression language {@value}.
	 */
	public static final String JAVA_SCRIPT_LANG = "javascript"; // NON-NLS
	/**
	 * Constant for name of scripting/expression language {@value}.
	 */
	public static final String XPATH_SCRIPT_LANG = "xpath"; // NON-NLS

	private static final String SCRIPTING_CFG_PROPERTIES = "scripting.properties"; // NON-NLS
	private static final String IMPORT_PACKAGES_PROP_KEY_SUFFIX = ".scripting.import.packages"; // NON-NLS
	private static final String IMPORT_CLASSES_PROP_KEY_SUFFIX = ".scripting.import.classes"; // NON-NLS

	private static final Pattern FIELD_VALUE_PLACEHOLDER_PATTERN = Pattern.compile("\\$(\\w+\\b)"); // NON-NLS
	private static final Pattern FIELD_PLACEHOLDER_PATTERN = Pattern.compile("\\{\\w+\\}"); // NON-NLS

	private static CompilerConfiguration DEFAULT_GROOVY_CONFIGURATION;
	private static String DEFAULT_JS_CODE_IMPORTS;

	private static final Set<String> DEFAULT_IMPORT_PACKAGES = new HashSet<>();
	private static final Set<String> DEFAULT_IMPORT_CLASSES = new HashSet<>();

	private static ScriptEngine GROOVY_SCRIPT_ENGINE;
	private static ScriptEngine JS_SCRIPT_ENGINE;

	static {
		initDefaultImportPackages();
	}

	private StreamsScriptingUtils() {
	}

	/**
	 * Initiates default set of Java API imported packages.
	 */
	private static void initDefaultImportPackages() {
		try {
			Properties p = Utils.loadPropertiesResources(SCRIPTING_CFG_PROPERTIES);

			for (String pName : p.stringPropertyNames()) {
				if (pName.endsWith(IMPORT_PACKAGES_PROP_KEY_SUFFIX)) {
					String importPackages = p.getProperty(pName);

					if (StringUtils.isNotEmpty(importPackages)) {
						String[] pArray = importPackages.split(";");

						Collections.addAll(DEFAULT_IMPORT_PACKAGES, pArray);
					}
				} else if (pName.endsWith(IMPORT_CLASSES_PROP_KEY_SUFFIX)) {
					String importClasses = p.getProperty(pName);

					if (StringUtils.isNotEmpty(importClasses)) {
						String[] cArray = importClasses.split(";");

						Collections.addAll(DEFAULT_IMPORT_CLASSES, cArray);
					}
				}
			}
		} catch (Exception exc) {
		}
	}

	/**
	 * Returns Groovy compiler configuration containing default set of Java API imported packages.
	 * 
	 * @return Groovy compiler compiler configuration containing default set of Java API imported packages
	 */
	public static CompilerConfiguration getDefaultGroovyCompilerConfig() {
		if (DEFAULT_GROOVY_CONFIGURATION == null) {
			DEFAULT_GROOVY_CONFIGURATION = initDefaultGroovyCompilerConfig();
		}

		return DEFAULT_GROOVY_CONFIGURATION;
	}

	private static CompilerConfiguration initDefaultGroovyCompilerConfig() {
		CompilerConfiguration cc = new CompilerConfiguration();

		// inject default imports
		ImportCustomizer ic = new ImportCustomizer();

		for (String pckg : DEFAULT_IMPORT_PACKAGES) {
			ic.addStarImports(pckg);
		}

		for (String pckg : DEFAULT_IMPORT_CLASSES) {
			ic.addImports(pckg);
		}

		cc.addCompilationCustomizers(ic);

		return cc;
	}

	/**
	 * Appends JS script code with default set of Java API imported packages.
	 *
	 * @param script
	 *            script code to wrap with default imports
	 * @return default imports appended JS script code
	 */
	public static String addDefaultJSScriptImports(String script) {
		if (DEFAULT_JS_CODE_IMPORTS == null) {
			DEFAULT_JS_CODE_IMPORTS = initDefaultJSScriptImports();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("var ScriptImports = new JavaImporter(\n");// NON-NLS
		sb.append(DEFAULT_JS_CODE_IMPORTS);
		sb.append(");\n"); // NON-NLS
		sb.append("with (ScriptImports) {\n"); // NON-NLS
		sb.append(script);
		sb.append("\n}"); // NON-NLS

		return sb.toString();
	}

	private static String initDefaultJSScriptImports() {
		StringBuilder sb = new StringBuilder();

		for (String pckg : DEFAULT_IMPORT_PACKAGES) {
			sb.append(sb.length() > 0 ? ", " : "  ").append(pckg).append("\n"); // NON-NLS
		}

		for (String cls : DEFAULT_IMPORT_CLASSES) {
			sb.append(sb.length() > 0 ? ", " : "  ").append(cls).append("\n"); // NON-NLS
		}

		return sb.toString();
	}

	/**
	 * Adds package name to default set of Java API imported packages.
	 * 
	 * @param pckg
	 *            package name to add
	 */
	public static void registerDefaultImportPackage(String pckg) {
		DEFAULT_IMPORT_PACKAGES.add(pckg);

		if (DEFAULT_GROOVY_CONFIGURATION != null) {
			List<CompilationCustomizer> ccList = DEFAULT_GROOVY_CONFIGURATION.getCompilationCustomizers();

			for (CompilationCustomizer cc : ccList) {
				if (cc instanceof ImportCustomizer) {
					((ImportCustomizer) cc).addStarImports(pckg);
				}
			}
		}

		if (DEFAULT_JS_CODE_IMPORTS != null) {
			StringBuilder sb = new StringBuilder(DEFAULT_JS_CODE_IMPORTS);

			sb.append(sb.length() > 0 ? ", " : "  ").append(pckg).append("\n"); // NON-NLS

			DEFAULT_JS_CODE_IMPORTS = sb.toString();
		}
	}

	/**
	 * Adds class name to default set of Java API imported packages.
	 *
	 * @param cls
	 *            class name to add
	 */
	public static void registerDefaultImportClass(String cls) {
		DEFAULT_IMPORT_CLASSES.add(cls);

		if (DEFAULT_GROOVY_CONFIGURATION != null) {
			List<CompilationCustomizer> ccList = DEFAULT_GROOVY_CONFIGURATION.getCompilationCustomizers();

			for (CompilationCustomizer cc : ccList) {
				if (cc instanceof ImportCustomizer) {
					((ImportCustomizer) cc).addImports(cls);
				}
			}
		}

		if (DEFAULT_JS_CODE_IMPORTS != null) {
			StringBuilder sb = new StringBuilder(DEFAULT_JS_CODE_IMPORTS);

			sb.append(sb.length() > 0 ? ", " : "  ").append(cls).append("\n"); // NON-NLS

			DEFAULT_JS_CODE_IMPORTS = sb.toString();
		}
	}

	/**
	 * Builds string, describing script based evaluation expression.
	 *
	 * @param userExpression
	 *            used defined evaluation expression
	 * @param vars
	 *            variables binding map
	 * @return string describing script based evaluation expression
	 */
	public static String describeExpression(String userExpression, Map<String, ?> vars, String lang,
			Collection<String> expVars, Map<String, String> phMap) {
		StringBuilder expDescStr = new StringBuilder();
		expDescStr.append("'").append(lang).append(":").append(userExpression).append("'"); // NON-NLS

		StringBuilder varStr = new StringBuilder();
		if (CollectionUtils.isNotEmpty(expVars)) {
			for (String eVar : expVars) {
				String vph = phMap.get(eVar);
				appendVariable(varStr, eVar, vars.get(vph));
			}
		}

		Object fValue = vars.get(FIELD_VALUE_VARIABLE_EXPR);
		if (fValue != null) {
			appendVariable(varStr, FIELD_VALUE_VARIABLE_EXPR, fValue);
		}

		if (varStr.length() > 0) {
			expDescStr.append(" (").append(varStr).append(")"); // NON-NLS
		}

		return expDescStr.toString();
	}

	/**
	 * Appends variable definition to expression description string builder <tt>sb</tt>.
	 *
	 * @param sb
	 *            string builder to append
	 * @param varName
	 *            variable name
	 * @param varValue
	 *            variable value
	 */
	public static void appendVariable(StringBuilder sb, String varName, Object varValue) {
		if (sb.length() > 0) {
			sb.append("; "); // NON-NLS
		}

		sb.append(Utils.getVarName(varName)).append("=").append(toString(varValue)); // NON-NLS
	}

	/**
	 * Returns the appropriate string representation for the specified object.
	 * <p>
	 * If <tt>obj</tt> is {@link String}, it gets surrounded by {@code "} chars. If <tt>obj</tt> is {@link Character},
	 * it gets surrounded by {@code '} character.
	 *
	 * @param obj
	 *            object to convert to string representation
	 * @return string representation of object
	 */
	public static String toString(Object obj) {
		if (obj instanceof String) {
			return Utils.surround(String.valueOf(obj), "\""); // NON-NLS
		}

		if (obj instanceof Character) {
			return Utils.surround(String.valueOf(obj), "'"); // NON-NLS
		}

		return Utils.toString(obj);
	}

	/**
	 * Compiles Groovy script code to be ready for later execution.
	 *
	 * @param scriptCode
	 *            Groovy script code string
	 * @return compiled instance of Groovy script code
	 * 
	 * @throws javax.script.ScriptException
	 *             if compilation fails
	 *
	 * @see javax.script.ScriptEngineManager#getEngineByName(String)
	 * @see javax.script.Compilable#compile(String)
	 */
	public static CompiledScript compileGroovyScript(String scriptCode) throws ScriptException {
		if (GROOVY_SCRIPT_ENGINE == null) {
			GROOVY_SCRIPT_ENGINE = initGroovyScriptEngine();
		}

		return ((Compilable) GROOVY_SCRIPT_ENGINE).compile(scriptCode);
	}

	private static ScriptEngine initGroovyScriptEngine() {
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName(GROOVY_LANG);
		((GroovyScriptEngineImpl) engine).setClassLoader(new GroovyClassLoader(
				Thread.currentThread().getContextClassLoader(), getDefaultGroovyCompilerConfig()));

		return engine;
	}

	/**
	 * Compiles JavaScript script code to be ready for later execution.
	 *
	 * @param scriptCode
	 *            JavaScript script code string
	 * @return compiled instance of JavaScript script code
	 *
	 * @throws javax.script.ScriptException
	 *             if compilation fails
	 *
	 * @see javax.script.ScriptEngineManager#getEngineByName(String)
	 * @see javax.script.Compilable#compile(String)
	 */
	public static CompiledScript compileJSScript(String scriptCode) throws ScriptException {
		if (JS_SCRIPT_ENGINE == null) {
			JS_SCRIPT_ENGINE = initJSScriptEngine();
		}

		return ((Compilable) JS_SCRIPT_ENGINE).compile(addDefaultJSScriptImports(scriptCode));
	}

	private static ScriptEngine initJSScriptEngine() {
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName(JAVA_SCRIPT_LANG);

		return engine;
	}

	/**
	 * Compiles provided script code to be ready for later execution.
	 * 
	 * @param lang
	 *            script code language
	 * @param scriptCode
	 *            script code string to compile
	 * @return compiled instance of script code
	 * 
	 * @throws ScriptException
	 *             if compilation fails or script language is not supported
	 * 
	 * @see #compileGroovyScript(String)
	 * @see #compileJSScript(String)
	 */
	public static CompiledScript compileScript(String lang, String scriptCode) throws ScriptException {
		switch (lang.toLowerCase()) {
		case GROOVY_LANG:
			return compileGroovyScript(scriptCode);
		case JAVA_SCRIPT_LANG:
			return compileJSScript(scriptCode);
		default:
			throw new ScriptException("Unknown script language"); // NON-NLS
		}
	}

	/**
	 * Checks if provided script expression string {@code expString} containing value reference placeholders (starting
	 * {@code '$'} symbol) matches valid script expression pattern:
	 * <ul>
	 * <li>expression referenced field value is defined using placeholder {@value #FIELD_VALUE_VARIABLE_EXPR} (case
	 * sensitive)</li>
	 * <li>expression referenced activity fields are defined using placeholder {@code ${FIELD_NAME}}, where
	 * {@code FIELD_NAME} is parser defined filed name or cache entry name</li>
	 * </ul>
	 *
	 * @param expString
	 *            script expression string to check
	 * @return {@code true} is provided script expression matches valid value reference placeholders pattern or
	 *         placeholders are not present in expression, {@code false} - if does not match or expression is
	 *         {@code null}
	 */
	public static boolean isScriptExpressionValid(String expString) {
		if (expString == null) {
			return false;
		}

		Matcher m = FIELD_PLACEHOLDER_PATTERN.matcher(expString);
		while (m.find()) {
			int phSIdx = m.start();

			if (phSIdx == 0 || (!isQuoted(expString, phSIdx) && isInvalidPlaceholder(expString, phSIdx))) {
				return false;
			}
		}

		m = FIELD_VALUE_PLACEHOLDER_PATTERN.matcher(expString);
		while (m.find()) {
			if (!FIELD_VALUE_VARIABLE_EXPR.equals(m.group())) {
				return false;
			}
		}

		return true;
	}

	private static boolean isInvalidPlaceholder(String expString, int phSIdx) {
		return isValidIdentifierStart(expString.charAt(phSIdx + 1))
				&& !isPlaceholderStart(expString.charAt(phSIdx - 1));
	}

	private static boolean isPlaceholderStart(char ch) {
		return ch == '$';
	}

	private static boolean isValidIdentifierStart(char ch) {
		return ch == '_' || Character.isLetter(ch);
	}

	private static boolean isQuoted(String expString, int phSIdx) {
		int qbc = countMatches(expString, phSIdx, '"');
		int nqIdx = expString.indexOf('"', phSIdx);

		return (qbc % 2 != 0) && nqIdx > phSIdx;
	}

	private static int countMatches(CharSequence str, int toIndex, char ch) {
		if (StringUtils.isEmpty(str)) {
			return 0;
		} else {
			int count = 0;

			for (int i = 0; i < toIndex; ++i) {
				if (ch == str.charAt(i)) {
					++count;
				}
			}

			return count;
		}
	}
}
