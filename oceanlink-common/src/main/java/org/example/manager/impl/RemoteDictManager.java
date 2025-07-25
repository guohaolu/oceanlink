package org.example.manager.impl;

import org.example.manager.IRemoteDictManager;
import org.example.pojo.dto.SysDictItemDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 远程数据字典服务
 *
 * @author guohao.lu
 */
@Service
public class RemoteDictManager implements IRemoteDictManager {
    @Override
    public List<SysDictItemDTO> listByType(String dictType) {
        return List.of();
    }
}
