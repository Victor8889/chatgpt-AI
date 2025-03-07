package com.hncboy.beehive.base.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ll
 * @date 2023/5/29
 * Cell 编码枚举
 */
@AllArgsConstructor
public enum CellCodeEnum {

    /**
     * 空
     */
    NULL("null"),

    /**
     * OpenAi 对话 Web GPT 3.5
     */
    OPENAI_CHAT_WEB_3_5("openai_chat_web_3_5"),

    /**
     * OpenAi 对话 Web GPT 4
     */
    OPENAI_CHAT_WEB_4("openai_chat_web_4"),

    /**
     * OpenAi 对话 Api GPT3.5
     */
    OPENAI_CHAT_API_3_5("openai_chat_api_3_5"),

    /**
     * OpenAi 对话 Api GPT4
     */
    OPENAI_CHAT_API_4("openai_chat_api_4"),

    /**
     * OpenAi 图像
     */
    OPENAI_IMAGE("openai_image"),

    /**
     * OpenAi 图像
     */
    OPENAI_DALLE3("openai_dalle3"),

    /**
     * NEW_BING
     */
    NEW_BING("new_bing"),

    /**
     * Midjourney
     */
    MIDJOURNEY("Midjourney"),

    /**
     * ChatGLM
     */
    CHAT_GLM("ChatGLM"),

    /**
     * POE
     */
    POE("poe"),

    /**
     * Azure
     */
    AZURE("Azure"),

    /**
     * Bard
     */
    Bard("Bard"),

    /**
     * 文心一言
     */
    BAIDU_WXYY("baidu_wxyy"),

    /**
     * 通义千问
     */
    ALI_TYQW("ali_tyqw");

    @Getter
    @JsonValue
    @EnumValue
    private final String code;

    /**
     * code 作为 key，封装为 Map
     */
    public static final Map<String, CellCodeEnum> CODE_MAP = Stream
            .of(CellCodeEnum.values())
            .collect(Collectors.toMap(cellCodeEnum -> cellCodeEnum.getCode().toLowerCase(), Function.identity()));

    /**
     * 静态工厂反序列化
     *
     * @param code code
     * @return CellCodeEnum
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CellCodeEnum valueOfKey(String code) {
        // 忽略大小写
        return Optional.ofNullable(CODE_MAP.get(code.toLowerCase()))
                .orElseThrow(() -> new IllegalArgumentException(code));
    }


}
