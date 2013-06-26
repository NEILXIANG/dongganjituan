package cn.halen.data.mapper;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.mybatis.spring.support.SqlSessionDaoSupport;

import cn.halen.data.pojo.MyOrder;
import cn.halen.data.pojo.MyRefund;
import cn.halen.data.pojo.MyTrade;
import cn.halen.util.Paging;

public class RefundMapper extends SqlSessionDaoSupport {

	public int insert(MyRefund myRefund) {
		int count = getSqlSession().insert("cn.halen.data.mapper.MyRefundMapper.insert", myRefund);
		return count;
	}
	
	public int insertMyOrder(MyOrder myOrder) {
		int count = getSqlSession().insert("cn.halen.data.mapper.MyTradeMapper.insertOrder", myOrder);
		return count;
	}
	
	public int insertRefund(MyRefund myRefund) {
		int count = getSqlSession().insert("cn.halen.data.mapper.MyTradeMapper.insertRefund", myRefund);
		return count;
	}
	
	public Long selectByTradeId(long id) {
		Long tradeId = getSqlSession().selectOne("cn.halen.data.mapper.MyTradeMapper.selectByTradeId", id);
		return tradeId;
	}
	
	public MyTrade selectTradeDetail(String id) {
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("tid", id);
		MyTrade myTrade = getSqlSession().selectOne("cn.halen.data.mapper.MyTradeMapper.selectTradeDetail", param);
		return myTrade;
	}
	
	public MyOrder selectOrderByOrderId(long oid) {
		MyOrder myOrder = getSqlSession().selectOne("cn.halen.data.mapper.MyTradeMapper.selectOrderByOrderId", oid);
		return myOrder;
	}
	
	public int updateTradeMemo(String memo, String tradeId, Date modified) {
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("memo", memo);
		param.put("tradeId", tradeId);
		param.put("modified", modified);
		int count = getSqlSession().update("cn.halen.data.mapper.MyTradeMapper.updateTradeMemo", param);
		return count;
	}
	
	public int updateMyTrade(MyTrade trade) {
		int count = getSqlSession().update("cn.halen.data.mapper.MyTradeMapper.updateTrade", trade);
		return count;
	}
	
	public int updateTradeStatus(int status, String tid) {
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("status", status);
		param.put("tid", tid);
		return getSqlSession().update("cn.halen.data.mapper.MyTradeMapper.updateTradeStatus", param);
	}
	
	public int updateMyOrder(MyOrder order) {
		int count = getSqlSession().update("cn.halen.data.mapper.MyTradeMapper.updateOrder", order);
		return count;
	}
	
	public int updateLogisticsAddress(String state, String city, String district, String address, String mobile, String phone,
			String zip, String name, Date modified, String tradeId) {
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("state", state);
		param.put("city", city);
		param.put("district", district);
		param.put("address", address);
		param.put("mobile", mobile);
		param.put("phone", phone);
		param.put("zip", zip);
		param.put("name", name);
		param.put("tradeId", tradeId);
		param.put("modified", modified);
		int count = getSqlSession().update("cn.halen.data.mapper.MyTradeMapper.updateLogisticsAddress", param);
		return count;
	}
	
	public long countRefund(List<String> sellerNickList, String tid, List<String> statusList) {
		Map<String, Object> param = new HashMap<String, Object>();
		if(null!=sellerNickList && sellerNickList.size()>0) {
			param.put("sellerNickList", sellerNickList);
		}
		if(StringUtils.isNotBlank(tid)) {
			param.put("tid", tid.trim());
		}
		if(null != statusList) {
			param.put("statusList", statusList);
		}
		Long count = getSqlSession().selectOne("cn.halen.data.mapper.MyRefundMapper.countRefund", param);
		return count;
	}
	
	public List<MyRefund> listRefund(List<String> sellerNickList, String tid, Paging paging, List<String> statusList) {
		Map<String, Object> param = new HashMap<String, Object>();
		if(null!=sellerNickList && sellerNickList.size()>0) {
			param.put("sellerNickList", sellerNickList);
		}
		if(StringUtils.isNotBlank(tid)) {
			param.put("tid", tid.trim());
		}
		if(null != paging) {
			param.put("start", paging.getStart());
			param.put("page_size", paging.getSize());
		}
		if(null != statusList) {
			param.put("statusList", statusList);
		}
		List<MyRefund> list = getSqlSession().selectList("cn.halen.data.mapper.MyRefundMapper.selectRefundMap", param);
		return list;
	}
}