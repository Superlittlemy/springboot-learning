package com.slm.rustfs.convert;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.slm.rustfs.entity.FileMetadata;
import com.slm.rustfs.model.FileResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FileConvert {
    
    FileResponse toResponse(FileMetadata fileMetadata);

}
