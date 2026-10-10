package com.slm.rustfs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slm.rustfs.entity.FileUploadTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileUploadTaskMapper extends BaseMapper<FileUploadTask> {

    /**
     * 任务完成后不再需要,物理删除
     */
    @Delete("DELETE FROM file_upload_task WHERE upload_id = #{uploadId}")
    int physicalDeleteByUploadId(@Param("uploadId") String uploadId);

}