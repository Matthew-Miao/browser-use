package com.mxy.browser.use.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.mxy.browser.use.action.*;
import com.mxy.browser.use.browser.Browser;
import com.mxy.browser.use.browser.BrowserContext;
import com.mxy.browser.use.browser.BrowserSession;
import com.mxy.browser.use.browser.BrowserState;
import com.mxy.browser.use.controller.Controller;
import com.mxy.browser.use.dom.DomElement;
import com.mxy.browser.use.dom.DomService;
import com.mxy.browser.use.dom.DomState;
import com.mxy.browser.use.memory.Memory;
import com.mxy.browser.use.memory.SimpleMemory;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 智能代理
 * 连接LLM与浏览器，实现自动化任务
 */
@Slf4j
public class Agent implements AutoCloseable {

    /**
     * 任务描述
     */
    private final String task;

    /**
     * LLM客户端
     */
    private final ChatClient llm;

    /**
     * 浏览器实例
     */
    private final Browser browser;

    /**
     * 浏览器上下文
     */
    private BrowserContext browserContext;

    /**
     * 控制器
     */
    private final Controller controller;

    /**
     * 内存
     */
    private final Memory memory;

    /**
     * 是否启用内存
     */
    private final boolean enableMemory;

    /**
     * 每步最大动作数
     */
    private final int maxActionsPerStep;

    /**
     * 最大步骤数
     */
    private final int maxSteps;

    /**
     * 系统提示模板
     */
    private final String systemPromptTemplate;

    /**
     * JSON解析器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 私有构造函数，使用Builder模式创建实例
     */
    @Builder
    private Agent(String task, ChatClient llm, Browser browser, boolean enableMemory,
                  int maxActionsPerStep, int maxSteps, Memory memory) {
        this.task = task;
        this.llm = llm;
        this.browser = browser != null ? browser : new Browser();
        this.controller = new Controller();
        this.enableMemory = enableMemory;
        this.maxActionsPerStep = maxActionsPerStep > 0 ? maxActionsPerStep : 3;
        this.maxSteps = maxSteps > 0 ? maxSteps : 20;
        this.memory = memory != null ? memory : new SimpleMemory();
        this.systemPromptTemplate = loadSystemPromptTemplate();
    }

    /**
     * 加载系统提示模板
     */
    private String loadSystemPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/system_prompt.txt");
            return new String(resource.getInputStream().readAllBytes());
        } catch (Exception e) {
            log.warn("无法加载系统提示模板，使用默认模板", e);
            return DEFAULT_SYSTEM_PROMPT;
        }
    }

    /**
     * 默认系统提示
     */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个浏览器自动化助手，可以控制浏览器执行各种任务。\n" +
                    "你将获得当前浏览器页面的状态，包括URL、标题、DOM元素等。\n" +
                    "你的任务是：<task>\n\n" +
                    "请根据当前状态，选择合适的下一步操作来完成任务。\n" +
                    "当前步骤：<step>/<maxSteps>\n\n" +
                    "可用操作:\n" +
                    "1. click(index): 点击指定索引的元素\n" +
                    "2. type(index, text): 在指定索引的元素中输入文本\n" +
                    "3. navigate(url): 导航到指定URL\n" +
                    "4. wait(seconds): 等待指定秒数\n" +
                    "5. done(success, message): 标记任务完成\n\n" +
                    "请按照以下JSON格式返回你的操作计划:\n" +
                    "{\n" +
                    "  \"reasoning\": \"这里描述你的思考过程，分析页面内容和要执行的任务\",\n" +
                    "  \"actions\": [\n" +
                    "    { \"type\": \"操作类型\", \"parameters\": { ... } },\n" +
                    "    ...\n" +
                    "  ]\n" +
                    "}";

    /**
     * 运行代理
     */
    public void run() {
        try {
            log.info("开始运行代理, 任务: {}", task);

            // 初始化浏览器上下文
            initializeBrowser();

            // 执行任务循环
            boolean done = false;
            int step = 0;

            while (!done && step < maxSteps) {
                step++;
                log.info("执行步骤 {}/{}", step, maxSteps);

                // 获取当前浏览器状态
                BrowserState state = getBrowserState();

                // 构建提示
                String prompt = buildPrompt(state, step);

                // 调用LLM获取响应
                String llmResponse = callLLM(prompt, step);

                // 解析LLM响应获取动作
                List<Action> actions = parseActions(llmResponse);

                // 限制每步最大动作数
                if (actions.size() > maxActionsPerStep) {
                    log.warn("动作数量超过限制，截断到 {} 个", maxActionsPerStep);
                    actions = actions.subList(0, maxActionsPerStep);
                }

                // 执行动作
                for (Action action : actions) {
                    log.info("执行动作: {}", action.getDescription());

                    ActionResult result = controller.executeAction(action, browserContext).get();

                    // 记录执行结果到内存
                    if (enableMemory) {
                        String memoryKey = "action_" + step + "_" + action.getType();
                        memory.add(memoryKey, result.isSuccess() ? "成功: " : "失败: " + result.getMessage());
                    }

                    log.info("动作结果: {} - {}", result.isSuccess() ? "成功" : "失败", result.getMessage());

                    // 如果是完成动作或执行失败，结束任务
                    if ("done".equals(action.getType()) || !result.isSuccess()) {
                        done = true;
                        break;
                    }

                    // 在动作之间稍作停顿
                    Thread.sleep(500);
                }
            }

            if (step >= maxSteps) {
                log.warn("达到最大步骤数 {}, 停止执行", maxSteps);
            }

            log.info("代理运行完成");
        } catch (Exception e) {
            log.error("代理运行失败", e);
            throw new RuntimeException("代理运行失败", e);
        }
    }

    /**
     * 初始化浏览器
     */
    private void initializeBrowser() throws ExecutionException, InterruptedException {
        log.debug("初始化浏览器");

        // 创建浏览器上下文
        browserContext = browser.newContext(null).get();

        // 初始化会话
        browserContext.initializeSession().get();
    }


    /**
     * 获取浏览器状态
     */
    /**
     * 获取浏览器状态
     */
    private BrowserState getBrowserState() throws ExecutionException, InterruptedException {
        // 获取浏览器会话
        BrowserSession session = browserContext.getSession().get();

        // 检查缓存状态
        BrowserState cachedState = session.getCachedState();

        if (cachedState == null ||
                cachedState.getUrl() == null ||
                cachedState.getTitle() == null ||
                cachedState.getSelectorMap() == null ||
                cachedState.getSelectorMap().isEmpty()) {

            log.info("缓存状态为空或不完整，尝试强制刷新...");

            try {
                // 获取当前页面
                Page currentPage = browserContext.getCurrentPage();
                if (currentPage != null
                        && currentPage.url() != null
                        && !currentPage.url().contains("about:blank")
                        && currentPage.title() != null
                        && !currentPage.title().isEmpty()) {

                    // 执行一个小动作来触发页面状态更新
                    currentPage.evaluate("window.scrollBy(0, 10);");

                    // 等待页面稳定
                    Thread.sleep(500);

                    // 创建DomService实例
                    DomService domService = new DomService(currentPage);

                    // 使用DomService获取可点击元素和DOM树
                    DomState domState = domService.getClickableElements(
                            true,  // 高亮元素
                            -1,    // 不设置焦点元素
                            500    // 视口扩展
                    ).get();

                    // 创建新的BrowserState
                    cachedState = new BrowserState();
                    cachedState.setUrl(currentPage.url());
                    cachedState.setTitle(currentPage.title());

                    // 设置DOM树和选择器映射
                    if (domState != null) {
                        cachedState.setElementTree(domState.getElementTree());
                        cachedState.setSelectorMap(domState.getSelectorMap());

                        log.info("DOM分析完成: 标题={}, URL={}, 可交互元素数量={}",
                                cachedState.getTitle(),
                                cachedState.getUrl(),
                                cachedState.getSelectorMap().size());
                    } else {
                        log.warn("DOM分析返回null");
                    }

                    // 更新缓存状态
                    browserContext.updateCachedState(cachedState);
                }
            } catch (Exception e) {
                log.error("尝试刷新状态失败", e, e);
            }
        }

        return cachedState;
    }

    /**
     * 构建LLM提示（用户部分）
     */
    /**
     * 构建LLM提示（用户部分）
     */
    private String buildPrompt(BrowserState state, int step) {
        // 构建用户提示
        StringBuilder userPromptBuilder = new StringBuilder();

        // 检查state是否为null
        if (state == null) {
            // 返回默认提示或错误信息
            userPromptBuilder.append("无法获取当前页面状态，浏览器可能未正确初始化或加载。\n");
            userPromptBuilder.append("步骤: ").append(step).append("\n");
            userPromptBuilder.append("任务: ").append(task).append("\n");
            return userPromptBuilder.toString();
        }

        // 添加当前页面信息
        userPromptBuilder.append("当前页面: ").append(state.getTitle())
                .append(" (URL: ").append(state.getUrl()).append(")\n\n");

        // 添加可交互元素列表
        userPromptBuilder.append("可交互元素:\n");
        if (state.getSelectorMap() != null && !state.getSelectorMap().isEmpty()) {
            for (Map.Entry<Integer, DomElement> entry : state.getSelectorMap().entrySet()) {
                DomElement element = entry.getValue();
                userPromptBuilder.append("[").append(entry.getKey()).append("] ");
                userPromptBuilder.append(element.getTagName()).append(": \"")
                        .append(element.getAllText()).append("\"");

                // 添加重要属性
                if (element.getId() != null && !element.getId().isEmpty()) {
                    userPromptBuilder.append(" (id=").append(element.getId()).append(")");
                }
                if (element.getClassName() != null && !element.getClassName().isEmpty()) {
                    userPromptBuilder.append(" (class=").append(element.getClassName()).append(")");
                }
                userPromptBuilder.append("\n");
            }
        } else {
            userPromptBuilder.append("(当前页面没有可交互元素)\n");
        }

        // 添加记忆内容（如果启用）
        if (enableMemory) {
            userPromptBuilder.append("\n历史操作:\n");
            for (String key : memory.keys()) {
                userPromptBuilder.append("- ").append(key).append(": ")
                        .append(memory.get(key)).append("\n");
            }
        }

        return userPromptBuilder.toString();
    }

    /**
     * 调用LLM
     */
    private String callLLM(String userPrompt, int step) {
        try {
            log.debug("调用LLM");

            // 准备系统提示模板变量
            Map<String, Object> variables = Map.of(
                    "task", task,
                    "step", String.valueOf(step),
                    "maxSteps", String.valueOf(maxSteps)
            );
            // 创建提示
            SystemMessage systemMessage = new SystemMessage(this.systemPromptTemplate);
            UserMessage userMessage = new UserMessage(userPrompt);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            // 调用LLM
            ChatClient.CallResponseSpec response = llm.prompt(prompt).call();
            String content = response.chatResponse().getResult().getOutput().getText();

            log.debug("LLM响应: {}", content);

            return content;
        } catch (Exception e) {
            log.error("调用LLM失败", e);
            throw new RuntimeException("调用LLM失败", e);
        }
    }

    /**
     * 解析LLM响应中的动作
     */
    private List<Action> parseActions(String response) {
        List<Action> actions = new ArrayList<>();

        try {
            // 提取JSON部分
            String jsonStr = extractJson(response);

            if (jsonStr == null) {
                log.warn("无法从LLM响应中提取JSON");
                // 默认返回一个等待动作
                actions.add(new WaitAction(3));
                return actions;
            }

            // 解析JSON
            JsonNode rootNode = objectMapper.readTree(jsonStr);

            // 提取reasoning（用于日志）
            if (rootNode.has("reasoning")) {
                String reasoning = rootNode.get("reasoning").asText();
                log.debug("LLM推理: {}", reasoning);
            }

            // 提取动作
            if (rootNode.has("actions") && rootNode.get("actions").isArray()) {
                JsonNode actionsNode = rootNode.get("actions");

                for (JsonNode actionNode : actionsNode) {
                    if (actionNode.has("type")) {
                        String type = actionNode.get("type").asText();
                        JsonNode parameters = actionNode.has("parameters") ?
                                actionNode.get("parameters") : null;

                        Action action = createAction(type, parameters);
                        if (action != null) {
                            actions.add(action);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析LLM响应失败", e);
            // 出错时返回一个等待动作
            actions.add(new WaitAction(3));
        }

        // 如果没有解析出动作，添加一个等待动作
        if (actions.isEmpty()) {
            actions.add(new WaitAction(3));
        }

        return actions;
    }

    /**
     * 从文本中提取JSON
     */
    private String extractJson(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // 查找JSON开始位置
        int start = text.indexOf('{');
        if (start == -1) {
            return null;
        }

        // 计数器
        int count = 1;
        int end = start + 1;

        // 查找匹配的结束花括号
        while (end < text.length() && count > 0) {
            char c = text.charAt(end);
            if (c == '{') {
                count++;
            } else if (c == '}') {
                count--;
            }
            end++;
        }

        if (count == 0) {
            return text.substring(start, end);
        }

        return null;
    }

    /**
     * 创建动作对象
     */
    private Action createAction(String type, JsonNode parameters) {
        try {
            switch (type.toLowerCase()) {
                case "click":
                    int clickIndex = parameters.get("index").asInt();
                    return new ClickAction(clickIndex);

                case "type":
                    int typeIndex = parameters.get("index").asInt();
                    String text = parameters.get("text").asText();
                    return new TypeAction(typeIndex, text);

                case "navigate":
                    String url = parameters.get("url").asText();
                    return new NavigateAction(url);

                case "wait":
                    int seconds = parameters.get("seconds").asInt();
                    return new WaitAction(seconds);

                case "done":
                    boolean success = parameters.has("success") ?
                            parameters.get("success").asBoolean() : true;
                    String message = parameters.has("message") ?
                            parameters.get("message").asText() : "任务完成";
                    return new DoneAction(success, message);

                default:
                    log.warn("未知的动作类型: {}", type);
                    return null;
            }
        } catch (Exception e) {
            log.error("创建动作对象失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        try {
            // 关闭浏览器上下文
            if (browserContext != null) {
                browserContext.close();
            }

            // 关闭浏览器
            if (browser != null) {
                browser.close().get();
            }

            // 清除内存
            if (memory != null) {
                memory.clearAll();
            }

            log.info("代理资源已释放");
        } catch (Exception e) {
            log.error("关闭代理资源失败", e);
        }
    }
} 