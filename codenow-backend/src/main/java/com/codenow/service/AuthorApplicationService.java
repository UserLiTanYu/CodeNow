package com.codenow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.AuthorApplicationDTO;
import com.codenow.dto.AuthorApplicationVO;
import com.codenow.entity.AuthorApplication;

/**
 * 作者申请服务接口，处理用户申请成为作者的全流程
 */
public interface AuthorApplicationService {

    /**
     * 提交作者申请
     *
     * @param userId 申请人用户ID
     * @param dto    申请信息
     * @return 申请记录
     */
    AuthorApplication submit(Long userId, AuthorApplicationDTO dto);

    /**
     * 查询用户最新的申请记录
     *
     * @param userId 用户ID
     * @return 最新申请记录视图对象，无记录时返回 null
     */
    AuthorApplicationVO latest(Long userId);

    /**
     * 分页查询当前用户的申请记录
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    Page<AuthorApplicationVO> pageMine(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 取消申请（仅待审核状态可取消）
     *
     * @param userId        用户ID
     * @param applicationId 申请记录ID
     */
    void cancel(Long userId, Long applicationId);

    /**
     * 管理员分页查询所有申请记录
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param status   状态筛选（可选）
     * @param keyword  搜索关键词（可选）
     * @return 分页结果
     */
    Page<AuthorApplicationVO> pageAdmin(Integer pageNum, Integer pageSize, String status, String keyword);

    /**
     * 管理员查看申请详情
     *
     * @param applicationId 申请记录ID
     * @return 申请详情视图对象
     */
    AuthorApplicationVO detail(Long applicationId);

    /**
     * 管理员批准申请
     *
     * @param applicationId 申请记录ID
     * @param reviewerId    审核人ID
     * @param remark        审核备注
     */
    void approve(Long applicationId, Long reviewerId, String remark);

    /**
     * 管理员驳回申请
     *
     * @param applicationId 申请记录ID
     * @param reviewerId    审核人ID
     * @param reason        驳回原因
     */
    void reject(Long applicationId, Long reviewerId, String reason);

    /**
     * 管理员撤销用户的作者身份
     *
     * @param userId     用户ID
     * @param reviewerId 审核人ID
     * @param reason     撤销原因
     */
    void revokeAuthor(Long userId, Long reviewerId, String reason);
}
