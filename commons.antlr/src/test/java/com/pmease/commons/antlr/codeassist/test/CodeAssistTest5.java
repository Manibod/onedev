package com.pmease.commons.antlr.codeassist.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.pmease.commons.antlr.codeassist.CodeAssist;
import com.pmease.commons.antlr.codeassist.ElementSpec;
import com.pmease.commons.antlr.codeassist.InputCompletion;
import com.pmease.commons.antlr.codeassist.InputStatus;
import com.pmease.commons.antlr.codeassist.InputSuggestion;
import com.pmease.commons.antlr.codeassist.LexerRuleRefElementSpec;
import com.pmease.commons.antlr.codeassist.LiteralElementSpec;
import com.pmease.commons.antlr.codeassist.Node;
import com.pmease.commons.antlr.codeassist.ParseTree;
import com.pmease.commons.antlr.codeassist.RuleRefElementSpec;

/**
 * This test case uses MySQL select grammar.
 * 
 * @author robin
 *
 */
public class CodeAssistTest5 {

	private static final Map<String, List<String>> SCHEMA_TABLES = new LinkedHashMap<>();
	
	private static final Map<String, List<String>> TABLE_COLUMNS = new LinkedHashMap<>();

	private static final String DEFAULT_SCHEMA = "schemaA"; 
	
	static {
		SCHEMA_TABLES.put("schemaA", Lists.newArrayList("tableA", "tableB"));
		SCHEMA_TABLES.put("schemaB", Lists.newArrayList("tableC", "tableD"));
		TABLE_COLUMNS.put("tableA", Lists.newArrayList("columnA", "columnB"));
		TABLE_COLUMNS.put("tableB", Lists.newArrayList("columnC", "columnD"));
		TABLE_COLUMNS.put("tableC", Lists.newArrayList("columnE", "columnF"));
		TABLE_COLUMNS.put("tableD", Lists.newArrayList("columnG", "columnH"));
	}
	
	private CodeAssist codeAssist = new CodeAssist(CodeAssistTest5Lexer.class, 
			new String[]{
					"com/pmease/commons/antlr/codeassist/test/CodeAssistTest5Lexer.g4", 
					"com/pmease/commons/antlr/codeassist/test/CodeAssistTest5Parser.g4"}, 
			"CodeAssistTest5Parser.tokens") {

		private static final long serialVersionUID = 1L;

		@Override
		protected List<InputSuggestion> suggest(final ParseTree parseTree, Node elementNode, String matchWith) {
			ElementSpec spec = (ElementSpec) elementNode.getSpec();
			if (spec instanceof RuleRefElementSpec) {
				RuleRefElementSpec ruleElementSpec = (RuleRefElementSpec) spec;
				if (ruleElementSpec.getRuleName().equals("schema_name")) {
					List<InputSuggestion> suggestions = new ArrayList<>();
					for (String schemaName: SCHEMA_TABLES.keySet()) {
						if (schemaName.startsWith(matchWith))
							suggestions.add(new InputSuggestion(schemaName));
					}
					return suggestions;
				}
			} else if (spec instanceof LexerRuleRefElementSpec) {
				LexerRuleRefElementSpec lexerRuleRefElementSpec = (LexerRuleRefElementSpec) spec;
				if ("tableName".equals(lexerRuleRefElementSpec.getLabel())) {
					List<InputSuggestion> suggestions = new ArrayList<>();
					Node columnNameNode = parseTree.findParentByRuleName(parseTree.getLastNode(), "column_name");
					String schemaName;
					List<Node> schemaNameNodes = parseTree.getChildrenByRuleName(columnNameNode, "schema_name", true);
					if (!schemaNameNodes.isEmpty())
						schemaName = parseTree.getText(schemaNameNodes.get(0));
					else
						schemaName = DEFAULT_SCHEMA;
					if (SCHEMA_TABLES.containsKey(schemaName)) {
						for (String tableName: SCHEMA_TABLES.get(schemaName)) {
							if (tableName.startsWith(matchWith))
								suggestions.add(new InputSuggestion(tableName));
						}
					}
					return suggestions;
				} else if ("columnName".equals(lexerRuleRefElementSpec.getLabel())) {
					List<InputSuggestion> suggestions = new ArrayList<>();
					Node columnNameNode = parseTree.findParentByRuleName(parseTree.getLastNode(), "column_name");
					List<Node> tableNameNodes = parseTree.getChildrenByLabel(columnNameNode, "tableName", true);
					if (!tableNameNodes.isEmpty()) {
						String tableName = parseTree.getText(tableNameNodes.get(0));
						if (TABLE_COLUMNS.containsKey(tableName)) {
							for (String columnName: TABLE_COLUMNS.get(tableName)) {
								if (columnName.startsWith(matchWith))
									suggestions.add(new InputSuggestion(columnName));
							}
						}
					}
					return suggestions;
				}
			} else if (spec instanceof LiteralElementSpec) {
				LiteralElementSpec literalElementSpec = (LiteralElementSpec) spec;
				if (literalElementSpec.getLiteral().length() == 1)
					return new ArrayList<>();
			}
			return null;
		}

	};
	
	private List<InputStatus> suggest(InputStatus inputStatus, String ruleName) {
		List<InputStatus> suggestions = new ArrayList<>();
		for (InputCompletion completion: codeAssist.suggest(inputStatus, ruleName))
			suggestions.add(completion.complete(inputStatus));
		return suggestions;
	}
	
	@Test
	public void test() {
		List<InputStatus> suggestions;
		
		suggestions = suggest(new InputStatus("select tableC."), "stat");
		assertEquals(2, suggestions.size());
		assertEquals("select tableC.columnE:21", suggestions.get(0).toString());
		assertEquals("select tableC.columnF:21", suggestions.get(1).toString());
		
		suggestions = suggest(new InputStatus("select schemaB."), "stat");
		assertEquals(2, suggestions.size());
		assertEquals("select schemaB.tableC.:22", suggestions.get(0).toString());
		assertEquals("select schemaB.tableD.:22", suggestions.get(1).toString());

		suggestions = suggest(new InputStatus("select schema"), "stat");
		assertEquals(2, suggestions.size());
		assertEquals("select schemaA.:15", suggestions.get(0).toString());
		assertEquals("select schemaB.:15", suggestions.get(1).toString());

		suggestions = suggest(new InputStatus("select table"), "stat");
		assertEquals(2, suggestions.size());
		assertEquals("select tableA.:14", suggestions.get(0).toString());
		assertEquals("select tableB.:14", suggestions.get(1).toString());
		
	}
	
}
