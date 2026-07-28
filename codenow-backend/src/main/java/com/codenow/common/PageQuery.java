package com.codenow.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询基类，封装通用的分页参数。
 * <p>
 * 所有需要分页的查询请求可继承此类，提供页码和每页条数两个基本参数。
 * </p>
 */
@Data
@Schema(description = "分页查询参数")
public class PageQuery {

    /** 当前页码，从 1 开始，默认为第 1 页 */
    @Schema(description = "当前页码", example = "1")
    private Integer pageNum = 1;

    /** 每页显示的记录条数，默认为 10 条 */
    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}
