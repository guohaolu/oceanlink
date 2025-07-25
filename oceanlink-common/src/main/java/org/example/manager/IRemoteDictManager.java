package org.example.manager;

import org.example.pojo.dto.SysDictItemDTO;

import java.util.List;

/**
 * 远程数据字典服务
 *
 * @author guohao.lu
 */
public interface IRemoteDictManager {
    List<SysDictItemDTO> listByType(String dictType);
}
