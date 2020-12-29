package com.mon4cc.service.impl;

import com.mon4cc.entity.Spout;
import com.mon4cc.mapper.SpoutMapper;
import com.mon4cc.service.ISpoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yangsanhe
 * @since 2020-12-19
 */
@Service
public class SpoutServiceImpl implements ISpoutService {

    @Autowired
    private SpoutMapper spoutMapper;

    @Override
    public boolean insert_batch(Spout spout) {
        spoutMapper.insertSpout(spout);
        return true;
    }

    @Override
    public boolean select_batch(String id,String topologyId) {
        if(spoutMapper.selectSpout(id,topologyId)!=null){
            return true;
        }else return false;
    }

    @Override
    public boolean update_batch(Spout spout) {
        spoutMapper.updateSpout(spout);
        return true;
    }

    @Override
    public List<Spout> selectSpouts(String topologyId) {
        return spoutMapper.selectSpouts(topologyId);
    }

    @Override
    public boolean updateCode(String id, String topologyId, String code) {
        return spoutMapper.updateSpoutCodeIntoSpoutTable(id,topologyId,code);
    }
}

