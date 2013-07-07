package cn.halen.data.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.support.SqlSessionDaoSupport;

import cn.halen.data.pojo.MySku;

public class MySkuMapper extends SqlSessionDaoSupport {

	public int insert(MySku sku) {
		int count = getSqlSession().insert("cn.halen.data.mapper.SkuMapper.insert", sku);
		return count;
	}
	
	public int delete(int id) {
		int count = getSqlSession().delete("cn.halen.data.mapper.SkuMapper.delete", id);
		return count;
	}
	
	public int update(MySku sku) {
		int count = getSqlSession().update("cn.halen.data.mapper.SkuMapper.update", sku);
		return count;
	}
	
	public MySku select(String goodsId, String color, String size) {
		Map<String, String> param = new HashMap<String, String>();
		param.put("goods_id", goodsId);
		param.put("color", color);
		param.put("size1", size);
		return getSqlSession().selectOne("cn.halen.data.mapper.SkuMapper.select", param);
	}
	
	public MySku select(long skuId) {
		return getSqlSession().selectOne("cn.halen.data.mapper.SkuMapper.selectBySkuId", skuId);
	}

    public List<MySku> selectByGoodsId(String goodsId) {
        return getSqlSession().selectList("cn.halen.data.mapper.SkuMapper.selectByGoodsId", goodsId);
    }

    public List<MySku> selectByGoodsIdColor(String goodsId, String color) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("goodsId", goodsId);
        param.put("color", color);
        return getSqlSession().selectList("cn.halen.data.mapper.SkuMapper.selectByGoodsIdColor", param);
    }

    public List<MySku> selectByGoodsIdSize(String goodsId, String size) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("goodsId", goodsId);
        param.put("size1", size);
        return getSqlSession().selectList("cn.halen.data.mapper.SkuMapper.selectByGoodsIdSize", param);
    }

    public int updateColorByGoodsIdColor(String goodsId, String color, String newColor) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("goodsId", goodsId);
        param.put("color", color);
        param.put("newColor", newColor);
        return getSqlSession().update("cn.halen.data.mapper.SkuMapper.updateColorByGoodsIdColor", param);
    }

    public int updateSizeByGoodsIdSize(String goodsId, String size, String newSize) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("goodsId", goodsId);
        param.put("size1", size);
        param.put("newSize", newSize);
        return getSqlSession().update("cn.halen.data.mapper.SkuMapper.updateSizeByGoodsIdSize", param);
    }
}
