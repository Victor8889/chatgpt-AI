package com.hncboy.beehive.cell.openai.module.chat.storage;

import com.hncboy.beehive.base.domain.entity.HaExpenseRecordsDo;
import com.hncboy.beehive.base.domain.entity.HaProductsDo;
import com.hncboy.beehive.base.domain.entity.HaUserPermissionsDo;
import com.hncboy.beehive.base.domain.entity.RoomOpenAiChatMsgDO;
import com.hncboy.beehive.base.enums.*;
import com.hncboy.beehive.cell.openai.enums.OpenAiChatApiModelEnum;
import com.hncboy.beehive.cell.openai.service.RoomOpenAiChatMsgService;
import cn.hutool.core.util.StrUtil;
import com.hncboy.beehive.web.service.HaExpenseRecordsService;
import com.hncboy.beehive.web.service.HaUserPermissionsService;
import com.unfbx.chatgpt.utils.TikTokensUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

/**
 * @author ll
 * @date 2023-3-25
 * ApiKey 数据库数据存储
 */
@Component
public class ApiKeyDatabaseDataStorage extends AbstractDatabaseDataStorage {

    @Resource
    private RoomOpenAiChatMsgService roomOpenAiChatMsgService;

    @Resource
    private HaExpenseRecordsService haExpenseRecordsService;
    @Resource
    private HaUserPermissionsService haUserPermissionsService;

    @Override
    public void onFirstMessage(RoomOpenAiChatMessageStorage chatMessageStorage) {
        RoomOpenAiChatMsgDO questionMessage = (RoomOpenAiChatMsgDO) chatMessageStorage.getQuestionMessageDO();
        RoomOpenAiChatMsgDO answerMessage = new RoomOpenAiChatMsgDO();
        answerMessage.setContent(chatMessageStorage.getReceivedMessage());
        answerMessage.setStatus(RoomOpenAiChatMsgStatusEnum.PART_SUCCESS);
        // 保存回答消息
        saveAnswerMessage(answerMessage, questionMessage, chatMessageStorage);

        // 设置回答消息
        chatMessageStorage.setAnswerMessageDO(answerMessage);

        // 更新问题消息状态为部分成功
        questionMessage.setStatus(RoomOpenAiChatMsgStatusEnum.PART_SUCCESS);
        roomOpenAiChatMsgService.updateById(questionMessage);
    }

    @Override
    void onLastMessage(RoomOpenAiChatMessageStorage chatMessageStorage) {
        RoomOpenAiChatMsgDO questionMessage = (RoomOpenAiChatMsgDO) chatMessageStorage.getQuestionMessageDO();
        RoomOpenAiChatMsgDO answerMessage = (RoomOpenAiChatMsgDO) chatMessageStorage.getAnswerMessageDO();

        // 成功状态
        questionMessage.setStatus(RoomOpenAiChatMsgStatusEnum.COMPLETE_SUCCESS);
        answerMessage.setStatus(RoomOpenAiChatMsgStatusEnum.COMPLETE_SUCCESS);

        // 原始响应数据
        answerMessage.setOriginalData(chatMessageStorage.getOriginalResponseData());
        answerMessage.setContent(chatMessageStorage.getReceivedMessage());

        // 填充使用 token
        populateMessageUsageToken(chatMessageStorage);

        // 更新消息
        roomOpenAiChatMsgService.updateById(questionMessage);
        HaProductsDo hpd = CommonEnum.NAME_MAP_PRODUCT.get(answerMessage.getModelName());
        //RecordsEnum.getByName(RecordsEnum.fromApiModel(answerMessage.getModelName()));


        if(answerMessage.getRoomId() == ChatGptRoomIdEnum.SWD_ROOM_ID.getCode())
            hpd = RecordsEnum.getByName(RecordsEnum.SIWEIDAOTU);

        HaUserPermissionsDo hsp = haUserPermissionsService.getOne(answerMessage.getUserId());
        int record = hpd.getRecords();
        if(hsp.getIsVip() == CommonEnum.isVip && (new Date()).before(hsp.getValidyDate())) {
            record = hpd.getVipRecords();
            if(OpenAiChatApiModelEnum.GPT_3_5_TURBO_1106.getName().equals(answerMessage.getModelName()) || OpenAiChatApiModelEnum.GPT_3_5_TURBO.getName().equals(answerMessage.getModelName())){
                if(hsp.getRemainHoldCount() >= record){
                    if(addFreeRecords(-record,hpd.getModel(),hpd.getName(),answerMessage.getId(),answerMessage.getUserId())){
                        if(haUserPermissionsService.updateFreePointCount(answerMessage.getUserId(),-record)) {
                            //RoomOpenAiChatMsgDO da = new RoomOpenAiChatMsgDO();
                            answerMessage.setIsDeducted(1);
                            //da.setId(answerMessage.getId());
                            //roomOpenAiChatMsgService.update(da);
                            //return;
                        }
                    }
                }
            }

        }
        if(addRecords(-record,hpd.getModel(),hpd.getName(),answerMessage.getId(),answerMessage.getUserId())){
            if(haUserPermissionsService.updatePoints(answerMessage.getUserId(),-record)) {
                //RoomOpenAiChatMsgDO da = new RoomOpenAiChatMsgDO();
                answerMessage.setIsDeducted(1);
                //da.setId(answerMessage.getId());
                //roomOpenAiChatMsgService.update(da);
            }
        }


        roomOpenAiChatMsgService.updateById(answerMessage);
    }

    private boolean addFreeRecords(int points,String model,String name,long modelId,int userId){
        HaExpenseRecordsDo herd = new HaExpenseRecordsDo();
        herd.setUserId(userId);
        herd.setModel(model);
        herd.setMark(name+"免费积分");
        herd.setHoldBi(points);
        herd.setModelId(modelId);

        return haExpenseRecordsService.save(herd);
    }
    private boolean addRecords(int points,String model,String name,long modelId,int userId){
        HaExpenseRecordsDo herd = new HaExpenseRecordsDo();
        herd.setUserId(userId);
        herd.setModel(model);
        herd.setMark(name);
        herd.setHoldBi(points);
        herd.setModelId(modelId);

        return haExpenseRecordsService.save(herd);
    }

    @Override
    void onErrorMessage(RoomOpenAiChatMessageStorage chatMessageStorage) {
        // 消息流条数大于 0 表示部分成功
        RoomOpenAiChatMsgStatusEnum roomOpenAiChatMsgStatusEnum = chatMessageStorage.getCurrentStreamMessageCount() > 0 ? RoomOpenAiChatMsgStatusEnum.PART_SUCCESS : RoomOpenAiChatMsgStatusEnum.ERROR;

        // 填充问题消息记录
        RoomOpenAiChatMsgDO questionMessage = (RoomOpenAiChatMsgDO) chatMessageStorage.getQuestionMessageDO();
        questionMessage.setStatus(roomOpenAiChatMsgStatusEnum);
        // 填充问题错误响应数据
        questionMessage.setResponseErrorData(chatMessageStorage.getErrorResponseData());

        // 填充使用 token
        populateMessageUsageToken(chatMessageStorage);

        // 还没收到回复就断了，跳过回答消息记录更新
        if (roomOpenAiChatMsgStatusEnum != RoomOpenAiChatMsgStatusEnum.ERROR) {
            // 填充问题消息记录
            RoomOpenAiChatMsgDO answerMessage = (RoomOpenAiChatMsgDO) chatMessageStorage.getAnswerMessageDO();
            answerMessage.setStatus(roomOpenAiChatMsgStatusEnum);
            // 原始响应数据
            answerMessage.setOriginalData(chatMessageStorage.getOriginalResponseData());
            // 错误响应数据
            answerMessage.setResponseErrorData(chatMessageStorage.getErrorResponseData());
            answerMessage.setContent(chatMessageStorage.getReceivedMessage());
            // 更新错误的回答消息记录
            roomOpenAiChatMsgService.updateById(answerMessage);
        } else {
            // 保存回答消息
            RoomOpenAiChatMsgDO answerMessage = new RoomOpenAiChatMsgDO();
            answerMessage.setStatus(RoomOpenAiChatMsgStatusEnum.ERROR);
            answerMessage.setOriginalData(chatMessageStorage.getOriginalResponseData());
            // 错误响应数据
            answerMessage.setResponseErrorData(chatMessageStorage.getErrorResponseData());
            // 解析内容填充
            answerMessage.setContent(chatMessageStorage.getParser().parseErrorMessage(answerMessage.getResponseErrorData()));

            // 返回给前端的错误信息从这里取
            chatMessageStorage.setAnswerMessageDO(answerMessage);
            chatMessageStorage.setReceivedMessage(answerMessage.getContent());
            saveAnswerMessage(answerMessage, questionMessage, chatMessageStorage);
        }

        // 更新错误的问题消息记录
        roomOpenAiChatMsgService.updateById(questionMessage);
    }

    /**
     * 保存回答消息
     *
     * @param answerMessage      回答消息
     * @param questionMessage    问题消息
     * @param chatMessageStorage 消息存储
     */
    private void saveAnswerMessage(RoomOpenAiChatMsgDO answerMessage, RoomOpenAiChatMsgDO questionMessage, RoomOpenAiChatMessageStorage chatMessageStorage) {
        answerMessage.setUserId(questionMessage.getUserId());
        answerMessage.setRoomId(questionMessage.getRoomId());
        answerMessage.setIp(questionMessage.getIp());
        answerMessage.setParentQuestionMessageId(questionMessage.getId());
        answerMessage.setMessageType(MessageTypeEnum.ANSWER);
        answerMessage.setModelName(questionMessage.getModelName());
        answerMessage.setApiKey(questionMessage.getApiKey());
        answerMessage.setOriginalData(chatMessageStorage.getOriginalResponseData());
        answerMessage.setPromptTokens(questionMessage.getPromptTokens());
        // 保存回答消息
        roomOpenAiChatMsgService.save(answerMessage);
    }

    /**
     * 填充消息使用 Token 数量
     *
     * @param chatMessageStorage 聊天消息数据存储
     */
    private void populateMessageUsageToken(RoomOpenAiChatMessageStorage chatMessageStorage) {
        Object answerMessageObj = chatMessageStorage.getAnswerMessageDO();
        if (Objects.isNull(answerMessageObj)) {
            return;
        }

        // 获取模型
        RoomOpenAiChatMsgDO questionMessage = (RoomOpenAiChatMsgDO) chatMessageStorage.getQuestionMessageDO();
        String modelName = questionMessage.getModelName();

        // 获取回答消耗的 tokens
        RoomOpenAiChatMsgDO answerMessage = (RoomOpenAiChatMsgDO) answerMessageObj;
        String answerContent = answerMessage.getContent();      //OpenAiChatApiModelEnum.NAME_MAP.get(modelName).getCalcTokenModelName()
        int completionTokens = StrUtil.isEmpty(answerContent) ? 0 : TikTokensUtil.tokens(CommonEnum.NAME_MAP_PRODUCT.get(questionMessage.getModelName()) .getApiModel(), answerContent);

        // 填充使用情况
        int totalTokens = questionMessage.getPromptTokens() + completionTokens;
        answerMessage.setPromptTokens(questionMessage.getPromptTokens());
        answerMessage.setCompletionTokens(completionTokens);
        answerMessage.setTotalTokens(totalTokens);

        questionMessage.setCompletionTokens(completionTokens);
        questionMessage.setTotalTokens(totalTokens);
    }
}
