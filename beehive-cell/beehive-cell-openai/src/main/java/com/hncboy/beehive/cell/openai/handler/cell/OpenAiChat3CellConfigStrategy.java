package com.hncboy.beehive.cell.openai.handler.cell;

import com.hncboy.beehive.base.enums.CellCodeEnum;
import com.hncboy.beehive.cell.core.hander.strategy.AbstractCellConfigStrategy;
import com.hncboy.beehive.cell.core.hander.strategy.ICellConfigCodeEnum;
import com.hncboy.beehive.cell.openai.enums.OpenAiChatCellConfigCodeEnum;
import org.springframework.stereotype.Component;

/**
 * @author ll
 * @date 2023/5/31
 * OpenAi 对话 GPT-3.5 配置项策略
 */
@Component
public class OpenAiChat3CellConfigStrategy extends AbstractCellConfigStrategy {

    @Override
    public CellCodeEnum getCellCode() {
        return CellCodeEnum.OPENAI_CHAT_API_3_5;
    }

    @Override
    public Class<? extends ICellConfigCodeEnum> getCellConfigCodeEnumClazz() {
        return OpenAiChatCellConfigCodeEnum.class;
    }
}
