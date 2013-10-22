package cn.halen.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.halen.data.mapper.*;
import cn.halen.data.pojo.*;
import cn.halen.service.*;
import cn.halen.service.excel.TradeExcelReader;
import cn.halen.service.excel.TradeRow;
import cn.halen.service.top.TradeClient;
import cn.halen.service.top.domain.TaoTradeStatus;
import cn.halen.util.Constants;
import com.taobao.api.domain.Trade;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.halen.controller.formbean.ClientOrder;
import cn.halen.exception.InsufficientBalanceException;
import cn.halen.exception.InsufficientStockException;
import cn.halen.exception.InvalidStatusChangeException;
import cn.halen.filter.UserHolder;
import cn.halen.service.top.TopConfig;
import cn.halen.service.top.util.MoneyUtils;

import com.taobao.api.ApiException;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class TradeActionController {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	@Autowired
	private GoodsMapper goodsMapper;
	
	@Autowired
	private MySkuMapper skuMapper;
	
	@Autowired
	private AdminMapper adminMapper;
	
	@Autowired
	private UtilService utilService;
	
	@Autowired
	private AreaMapper areaMapper;

    @Autowired
    private TradeClient tradeClient;
	
	@Autowired
	private MyLogisticsCompanyMapper myLogisticsCompanyMapper;
	
	@Autowired
	private TradeService tradeService;

    @Autowired
    MyTradeMapper tradeMapper;
	
	@Autowired
	private TopConfig topConfig;

    @Autowired
    private SkuService skuService;

    @Autowired
    private AdminService adminService;
	
	@SuppressWarnings("rawtypes")
	@Autowired
	private RedisTemplate redisTemplate;
	
	private ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<String, String>();
	
	private static final String REDIS_LOGISTICS_CODE = "redis:code";
	
	private static final String REDIS_AREA = "redis:area";

    @RequestMapping(value="trade/action/upload")
    public String upload(Model model) {
        return "trade/upload";
    }

    @RequestMapping(value="trade/action/add_comment_form")
    public String addCommentForm(Model model, @RequestParam String id, @RequestParam String type, @RequestParam(required = false) String from) {
        MyTrade trade = tradeMapper.selectById(id);
        model.addAttribute("trade", trade);
        model.addAttribute("logistics", myLogisticsCompanyMapper.list());
        model.addAttribute("type", type);
        model.addAttribute("from", from);
        return "trade/add_comment_form";
    }

    @RequestMapping(value="trade/action/add_comment")
    public void addComment(Model model, @RequestParam String id, @RequestParam String comment, @RequestParam String type, @RequestParam(required = false) String from,
                            HttpServletResponse resp) {
        MyTrade trade = tradeMapper.selectById(id);
        if("kefu_memo".equals(type)) {
            trade.setKefu_memo(comment.trim());
        } else if("cangku_memo".equals(type)) {
            trade.setCangku_memo(comment.trim());
        } else if("kefu_msg".equals(type)) {
            trade.setKefu_msg(comment.trim());
        } else if("cangku_msg".equals(type)) {
            trade.setCangku_msg(comment.trim());
        }
        tradeMapper.updateMyTrade(trade);
        try {
            if("list".equals(from)) {
                resp.sendRedirect("/trade/trade_list?isCancel=0&isSubmit=0&isFinish=0&map=true");
            } else {
                resp.sendRedirect("/trade/trade_detail?id=" + id);
            }
        } catch (IOException e) {
        }
    }

    @RequestMapping(value="trade/action/cancel_trade_form")
    public String cancelTradeForm(Model model, @RequestParam String id, @RequestParam(value="isApply", required=false) String isApply) {
        MyTrade trade = tradeMapper.selectById(id);
        model.addAttribute("trade", trade);
        model.addAttribute("logistics", myLogisticsCompanyMapper.list());
        model.addAttribute("isApply", isApply);
        return "trade/cancel_trade_form";
    }

    @RequestMapping(value="trade/action/cancel_trade")
    public void cancelTrade(Model model, @RequestParam String id, @RequestParam(value="why-cancel", required = false) String whyCancel, @RequestParam(value="isApply", required=false) String isApply,
                                HttpServletResponse resp) throws InsufficientStockException {
        MyTrade trade = tradeMapper.selectTradeMap(id);
        if("true".equals(isApply)) {
            trade.setIs_cancel(-1);
        } else {
            trade.setIs_cancel(1);
        }
        if(StringUtils.isNotBlank(whyCancel)) {
            trade.setWhy_cancel(whyCancel.trim());
        }
        tradeService.cancel(trade);
        try {
            resp.sendRedirect("/trade/trade_detail?id=" + id);
        } catch (IOException e) {
        }
    }

    @RequestMapping(value="trade/action/do_upload")
    public String doUpload(Model model, @RequestParam("file") MultipartFile file) {
        if(!file.isEmpty()) {
            String type = file.getContentType();
            if(!"application/vnd.ms-excel".equals(type)) {
                model.addAttribute("errorInfo", "选择的文件必须是03版本的excel表格!");
                return "trade/upload";
            }
            File dest = null;
            try {
                String fileName = new String(file.getOriginalFilename().getBytes("iso8859-1"), "UTF-8");
                dest = new File(topConfig.getFileBatchTrade() + "/" + fileName);
                if(dest.exists()) {
                    model.addAttribute("errorInfo", "这个文件已经存在，不能重复添加!");
                    return "trade/upload";
                }
                byte[] bytes = file.getBytes();
                OutputStream out = new FileOutputStream(dest);
                out.write(bytes);
                out.flush();
                out.close();
                boolean handleResult = handleExcel(model, dest);
                if(!handleResult) {
                    dest.delete();
                    return "trade/upload";
                }
            } catch (Exception e) {
                dest.delete();
                log.error("Upload file failed, ", e);
                model.addAttribute("errorInfo", "上传文件失败，请重试!");
                return "trade/upload";
            }
        } else {
            model.addAttribute("errorInfo", "必须选择一个文件!");
            return "trade/upload";
        }
        model.addAttribute("successInfo", "批量导入订单成功!");
        return "trade/upload";
    }

    private boolean handleExcel(Model model, File file) throws ApiException {
        TradeExcelReader reader = null;
        List<TradeRow> rows = null;
        try {
            reader = new TradeExcelReader(file);
            boolean checkColumn = reader.checkColumn();
            if(!checkColumn) {
                model.addAttribute("errorInfo", "格式不正确，请选择正确的文件!");
                return false;
            }
            rows = reader.getData();
        } catch (Exception e) {
            log.error("Handle excel failed, ", e);
            model.addAttribute("errorInfo", "系统异常，请联系管理员!");
            return false;
        } finally {
            reader.destroy();
        }

        String sellerNick = UserHolder.get().getShop().getSellerNick();

        List<MyTrade> tList = tradeService.toMyTrade(rows, sellerNick);
        List<Integer> lost = skuService.checkExist(tList);
        List<String> repeated = new ArrayList<String>();
        List<String> successed = new ArrayList<String>();
        for(MyTrade t : tList) {
            t.setStatus(TradeStatus.WaitReceive.getStatus());
            t.setIs_finish(1);
            int result = tradeService.insertMyTrade(t, true, Constants.QUANTITY, null);
            if(0==result) {
                repeated.add(t.getTid());
            } else {
                successed.add(t.getTid());
            }
        }
        model.addAttribute("lost", lost);
        model.addAttribute("repeated", repeated);
        model.addAttribute("successed", successed);

        return true;
    }

	@RequestMapping(value="trade/action/buy_goods_form")
	public String buyGoodsForm(Model model, @RequestParam(value="orders", required=false) String orders, 
			@RequestParam(value="fromcart", required=false) String fromCart,
            @RequestParam(value="tid", required=false) String tid,
            @RequestParam(value="addGoods", required=false) String addGoods,
            @RequestParam(required = false) String from) {
		User user = UserHolder.get();
		String token = user.getUsername() + System.currentTimeMillis();
		tokens.put(token, "ture");
		model.addAttribute("token", token);
		
		if(null != fromCart) {
			model.addAttribute("fromcart", true);
			model.addAttribute("logistics", myLogisticsCompanyMapper.list());
			return "trade/buy_goods_form";
		}
		if(StringUtils.isEmpty(orders)) {
			model.addAttribute("errorInfo", "请选择要购买的商品！");
			return "error_page";
		}
		List<ClientOrder> orderList = new ArrayList<ClientOrder>();
		
		String[] orderArr = orders.split(":::");
		for(String order : orderArr) {
			String[] items = order.split(",");
			if(items.length != 6) {
				continue;
			}
			ClientOrder clientOrder = new ClientOrder();
			clientOrder.setGoodsId(items[0]);
			clientOrder.setUrl(items[1]);
			clientOrder.setTitle(items[2]);
			clientOrder.setColor(items[3]);
			clientOrder.setSize(items[4]);
			clientOrder.setCount(Integer.parseInt(items[5]));
			orderList.add(clientOrder);
		}
        if(null != addGoods && "true".equals(addGoods)) {
            model.addAttribute("addGoods", true);
        } else {
            model.addAttribute("addGoods", false);
        }
		model.addAttribute("orderList", orderList);
		model.addAttribute("logistics", myLogisticsCompanyMapper.list());
        model.addAttribute("tid", tid);
        model.addAttribute("from", from);

		return "trade/buy_goods_form";
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value="trade/action/buy_goods")
	public String buyGoods(Model model, HttpServletRequest req, HttpServletResponse resp) {
		String token = req.getParameter("token");
		if(tokens.get(token) == null) {
			model.addAttribute("errorInfo", "请不要重复提交表单！");
			return "error_page";
		}
		
		String logistics = req.getParameter("logistics");
		String province = req.getParameter("province");
		String city = req.getParameter("city");
		String district = req.getParameter("district");
		String address = req.getParameter("address");
		String receiver = req.getParameter("receiver");
		String phone = req.getParameter("phone");
		String mobile = req.getParameter("mobile");
		String errorInfo = validateAddress(model, province, city, district, address, receiver, mobile);
		if(null != errorInfo) {
			model.addAttribute("errorInfo", errorInfo);
			return "error_page";
		}
		String postcode = req.getParameter("postcode");
		String sellerMemo = req.getParameter("seller_memo");
	
		MyTrade trade = new MyTrade();
        String id = tradeMapper.generateId();
        trade.setId(id);
		
		String logisticsCompany = (String) redisTemplate.opsForValue().get(REDIS_LOGISTICS_CODE + ":" + logistics);
		if(null == logisticsCompany) {
			logisticsCompany = myLogisticsCompanyMapper.selectByCode(logistics).getName();
			redisTemplate.opsForValue().set(REDIS_LOGISTICS_CODE + ":" + logistics, logisticsCompany);
		}
		trade.setDelivery(logisticsCompany);

		String provinceName = (String) redisTemplate.opsForValue().get(REDIS_AREA + ":" + province);
		if(null == provinceName) {
			provinceName = areaMapper.selectById(Long.parseLong(province)).getName();
			redisTemplate.opsForValue().set(REDIS_AREA + ":" + province, provinceName);
		}
		trade.setState(provinceName);
		
		String cityName = (String) redisTemplate.opsForValue().get(REDIS_AREA + ":" + city);
		if(null == cityName) {
			cityName = areaMapper.selectById(Long.parseLong(city)).getName();
			redisTemplate.opsForValue().set(REDIS_AREA + ":" + city, cityName);
		}
		trade.setCity(cityName);
		
		String districtName = (String) redisTemplate.opsForValue().get(REDIS_AREA + ":" + district);
		if(null == districtName) {
			districtName = areaMapper.selectById(Long.parseLong(district)).getName();
			redisTemplate.opsForValue().set(REDIS_AREA + ":" + district, districtName);
		}
		trade.setDistrict(districtName);
		trade.setAddress(address);
		trade.setPostcode(postcode);
		trade.setSeller_memo(sellerMemo);
		trade.setName(receiver);
		trade.setPhone(phone);
		trade.setMobile(mobile);
		trade.setStatus(TradeStatus.UnSubmit.getStatus());
		trade.setCome_from(Constants.MANAUAL);
        trade.setPay_type(Constants.PAY_TYPE_ONLINE);
		
		boolean hasNext = true;
		
		List<MyOrder> orderList = new ArrayList<MyOrder>();
		int payment = 0;
		
		User currentUser = UserHolder.get();
		float discount = currentUser.getShop().getD().getDiscount();
		trade.setSeller_nick(currentUser.getShop().getSellerNick());

		int count = 0;
		String goodsId = null;
		while(hasNext) {
			String currGoodsId = req.getParameter("goods" + count);
			if(null == currGoodsId) {
				hasNext = false;
				break;
			}
			goodsId = currGoodsId;
			int quantity = 0;
			try {
				quantity = Integer.parseInt(req.getParameter("count" + count));
			} catch(Exception e) {
				model.addAttribute("errorInfo", "商品数量必须填写数字！");
				return "error_page";
			}
			if(quantity <= 0) {
				model.addAttribute("errorInfo", "商品数量不能小于0！");
				return "error_page";
			}
			
			Goods goods = goodsMapper.getByHid(goodsId);
			String color = req.getParameter("color" + count);
			String size = req.getParameter("size" + count);
			
			MyOrder myOrder = new MyOrder();
			myOrder.setGoods_id(goodsId);
			myOrder.setColor(color);
			myOrder.setSize(size);
            MySku sku = skuMapper.select(goodsId, color, size);
            myOrder.setSku_id(sku.getId());
			myOrder.setQuantity(quantity);
			int singlePayment = MoneyUtils.cal(goods.getPrice(), discount, quantity);
			payment += singlePayment;
			myOrder.setPayment(singlePayment);
			myOrder.setTitle(goods.getTitle());
			myOrder.setPic_path(goods.getUrl());
			myOrder.setStatus(TaoTradeStatus.WAIT_SELLER_SEND_GOODS.getValue());
			myOrder.setTid(id);
			orderList.add(myOrder);
			count++;
		}
		trade.setMyOrderList(orderList);
		trade.setPayment(payment);
		
		int totalGoods = orderList.size();
		trade.setGoods_count(totalGoods);
		
		trade.setDelivery_money(utilService.calDeliveryMoney(goodsId, totalGoods, logistics, province));

        Map<String, String> idHolder  = new HashMap<String, String>();
		try{
			tradeService.insertMyTrade(trade, false, Constants.LOCK_QUANTITY, idHolder);
		} catch(Exception e) {
			log.error("", e);
			model.addAttribute("errorInfo", "系统异常，请重试！");
			return "error_page";
		}
		tokens.remove(token);
        try {
            resp.sendRedirect("/trade/trade_detail?id=" + idHolder.get("id"));
            return null;
        } catch (IOException e) {
        }
        return null;
	}

    @SuppressWarnings("unchecked")
    @RequestMapping(value="trade/action/add_goods")
    public String addGoods(Model model, HttpServletRequest req, HttpServletResponse resp, @RequestParam(required = false) String from) {
        String token = req.getParameter("token");
        if(tokens.get(token) == null) {
            model.addAttribute("errorInfo", "请不要重复提交表单！");
            return "error_page";
        }

        String tid = req.getParameter("tid");
        boolean hasNext = true;

        List<MyOrder> orderList = new ArrayList<MyOrder>();
        int count = 0;
        String goodsId = null;
        while(hasNext) {
            String currGoodsId = req.getParameter("goods" + count);
            if(null == currGoodsId) {
                hasNext = false;
                break;
            }
            goodsId = currGoodsId;
            int quantity = 0;
            try {
                quantity = Integer.parseInt(req.getParameter("count" + count));
            } catch(Exception e) {
                model.addAttribute("errorInfo", "商品数量必须填写数字！");
                return "error_page";
            }
            if(quantity <= 0) {
                model.addAttribute("errorInfo", "商品数量不能小于0！");
                return "error_page";
            }

            Goods goods = goodsMapper.getByHid(goodsId);
            String color = req.getParameter("color" + count);
            String size = req.getParameter("size" + count);

            MyOrder myOrder = new MyOrder();
            myOrder.setGoods_id(goodsId);
            myOrder.setColor(color);
            myOrder.setSize(size);
            MySku sku = skuMapper.select(goodsId, color, size);
            myOrder.setSku_id(sku.getId());
            myOrder.setQuantity(quantity);
            myOrder.setTitle(goods.getTitle());
            myOrder.setPic_path(goods.getUrl());
            myOrder.setTid(tid);
            orderList.add(myOrder);
            count++;
        }
        try{
            for(MyOrder order : orderList) {
                tradeMapper.insertMyOrder(order);
            }
        } catch(Exception e) {
            log.error("", e);
            model.addAttribute("errorInfo", "系统异常，请重试！");
            return "error_page";
        }
        tokens.remove(token);
        try {
            if("list".equals(from)) {
                resp.sendRedirect("/trade/trade_list?isCancel=0&isSubmit=0&isFinish=0&map=true");
            } else {
                resp.sendRedirect("/trade/trade_detail?id=" + tid);
            }
            return null;
        } catch (IOException e) {
        }
        return null;
    }

    @RequestMapping(value="trade/action/del_goods")
    public void delGoods(Model model, HttpServletResponse resp, @RequestParam long oid, @RequestParam String tid, @RequestParam(required = false) String from) {
        tradeMapper.delOrder(oid);
        try {
            if("list".equals(from)) {
                resp.sendRedirect("/trade/trade_list?isCancel=0&isSubmit=0&isFinish=0&map=true");
            } else {
                resp.sendRedirect("/trade/trade_detail?id=" + tid);
            }
        } catch (IOException e) {
        }
    }
	
	@RequestMapping(value="trade/action/shopcart")
	public String shopCart(Model model, HttpServletRequest req) {
		return "trade/shop_cart";
	}
	
	public String generateTradeId() {
		UUID uuid = UUID.randomUUID();  
        String str = uuid.toString();  
        String temp = str.substring(0, 8) + str.substring(9, 13) + str.substring(14, 18) + str.substring(19, 23) + str.substring(24);  
        return temp; 
	}
	
	@RequestMapping(value="trade/action/batch_change_status")
	public @ResponseBody ResultInfo batchChangeStatus(Model model, @RequestParam("tids") String tids, @RequestParam("action") String action) {
		ResultInfo result = new ResultInfo();
        boolean allSuccess = true;
		if(StringUtils.isNotEmpty(tids)) {
			String[] tidArr = tids.split(";");
			try {
				if(action.equals("approve1")) {
					for(String tid : tidArr) {
						if(StringUtils.isNotEmpty(tid)) {
							tradeService.approve1(tid);
						}
					}
				} else if(action.equals("submit")) {
					for(String tid : tidArr) {
						if(StringUtils.isNotEmpty(tid)) {
							boolean b = tradeService.submit(tid);
                            if(!b) {
                                allSuccess = false;
                            }
						}
					}
				} else if(action.equals("find-goods")) {
					for(String tid : tidArr) {
						if(StringUtils.isNotEmpty(tid)) {
							tradeService.findGoods(tid);
						}
					}
				} 
			} catch (InvalidStatusChangeException isce) {
				result.setSuccess(false);
				result.setErrorInfo("这个订单" + isce.getTid() + "不能进行此操作!");
			} catch(InsufficientBalanceException ibe) {
				log.error("", ibe);
				result.setSuccess(false);
				result.setErrorInfo("余额不足，请打款！");
			} catch (Exception e) {
				log.error("", e);
				result.setSuccess(false);
				result.setErrorInfo("系统异常，请重试!");
			}
		}
        if(!allSuccess) {
            result.setSuccess(false);
            result.setErrorInfo("部分订单请求失败!");
        }
		return result;
	}
	
	@RequestMapping(value="trade/action/change_status")
	public @ResponseBody ResultInfo changeStatus(Model model, @RequestParam("tid") String tid,
                                                 @RequestParam("oid") String oid, @RequestParam("action") String action) {
		ResultInfo result = new ResultInfo();
		try {
			if(action.equals("approve1")) {
				tradeService.approve1(tid);
			} else if(action.equals("submit")) {
				tradeService.submit(tid);
			} else if(action.equals("find-goods")) {
				tradeService.findGoods(tid);
			} else if(action.equals("no-goods")) {
				tradeService.noGoods(tid, oid);
			} else if(action.equals("refund-success")) {
				tradeService.refundSuccess(tid);
			}
		} catch (InvalidStatusChangeException isce) {
			result.setSuccess(false);
			result.setErrorInfo("这个订单不能进行此操作!");
		} catch(InsufficientStockException ise) {
			result.setSuccess(false);
			result.setErrorInfo("库存不足，不能购买！");
		} catch(InsufficientBalanceException ibe) {
			log.error("", ibe);
			result.setSuccess(false);
			result.setErrorInfo("余额不足，请打款！");
		} catch (Exception e) {
			log.error("", e);
			result.setSuccess(false);
			result.setErrorInfo("系统异常，请重试!");
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value="trade/action/change_delivery")
	public @ResponseBody ResultInfo changeDelivery(Model model, @RequestParam("id") String id, @RequestParam("delivery") String delivery) {
		ResultInfo result = new ResultInfo();
		try {
			String logisticsCompany = (String) redisTemplate.opsForValue().get(REDIS_LOGISTICS_CODE + ":" + delivery);
			if(null == logisticsCompany) {
				logisticsCompany = myLogisticsCompanyMapper.selectByCode(delivery).getName();
				redisTemplate.opsForValue().set(REDIS_LOGISTICS_CODE + ":" + delivery, logisticsCompany);
			}
			result.setErrorInfo(logisticsCompany);
            MyTrade trade = tradeMapper.selectTradeMap(id);
			int deliveryMoney = 0;
            int quantity = 0;
            for(MyOrder order : trade.getMyOrderList()) {
                quantity += order.getQuantity();
            }
            String goodsId = trade.getMyOrderList().get(0).getGoods_id();
            if(StringUtils.isNotBlank(trade.getState())) {
                deliveryMoney = utilService.calDeliveryMoney(goodsId, quantity, delivery, trade.getState());
            }

			tradeService.changeDelivery(id, logisticsCompany, deliveryMoney);
		} catch (InvalidStatusChangeException isce) {
			result.setSuccess(false);
			result.setErrorInfo("这个订单不能进行此操作!");
		} catch (InsufficientBalanceException ibe) {
			result.setSuccess(false);
			result.setErrorInfo("余额不足，不能进行此操作!");
		} catch (Exception e) {
			log.error("", e);
			result.setSuccess(false);
			result.setErrorInfo("系统异常，请重试!");
		}
		return result;
	}

    @RequestMapping(value="trade/action/change_delivery_number")
    public @ResponseBody ResultInfo changeDeliveryNumber(Model model, @RequestParam("id") String id, @RequestParam() String deliveryNumber) {
        ResultInfo result = new ResultInfo();
        MyTrade trade = tradeMapper.selectById(id);
        trade.setDelivery_number(deliveryNumber);
        tradeMapper.updateMyTrade(trade);
        result.setErrorInfo(deliveryNumber);
        return result;
    }

    /**
     * 扫描单号
     * @param model
     * @return
     */
	@RequestMapping(value="trade/batch_add_tracking_number")
	public @ResponseBody ResultInfo batchAddTrackingNumber(Model model, @RequestParam() String param) {
		ResultInfo result = new ResultInfo();
		try {
            if(StringUtils.isBlank(param)) {
                return result;
            }
            String[] trades = param.split(",");
            for(String trade : trades) {
                if(StringUtils.isBlank(trade)) {
                    continue;
                }
                String[] items = trade.split(":");
                if(items.length != 2) {
                    continue;
                }
                if(StringUtils.isBlank(items[0]) || StringUtils.isBlank(items[1])) {
                    continue;
                }

                tradeService.addTrackingNumber(items[0], items[1]);
            }
		} catch (Exception e) {
			result.setSuccess(false);
			result.setErrorInfo("系统异常，请重试!");
			return result;
		}
		return result;
	}

    @RequestMapping(value="trade/add_tracking_number")
    public @ResponseBody ResultInfo addTrackingNumber(Model model, @RequestParam("tid") String tid,
                                                      @RequestParam("trackingNumber") String trackingNumber) {
        ResultInfo result = new ResultInfo();
        try {
            tradeService.addTrackingNumber(tid, trackingNumber);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorInfo("系统异常，请重试!");
            return result;
        }
        return result;
    }

    /**
     * 发放单号
     * @param model
     * @return
     */
    @RequestMapping(value="trade/action/delivery_tracking_number")
    public @ResponseBody ResultInfo send(Model model, @RequestParam String ids) {

        ResultInfo result = new ResultInfo();
        if(StringUtils.isNotEmpty(ids)) {
            String[] idArr = ids.split(";");
            try {
                for(String id : idArr) {
                    if(StringUtils.isNotEmpty(id)) {
                        String errorInfo = null;
                        try {
                            errorInfo = tradeService.send(id);
                        } catch (Exception e) {
                            log.error("send error, ", e);
                        }
                        if(StringUtils.isNotBlank(errorInfo)) {
                            result.setSuccess(false);
                            result.setErrorInfo(errorInfo);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("", e);
                result.setSuccess(false);
                result.setErrorInfo("系统异常，请重试!");
            }
        }
        return result;
    }
	
	private String validateAddress(Model model, String province, String city, String district, String address
			, String receiver, String mobile) {
		String errorInfo = null;
		if(province.equals("-1")) {
			errorInfo = "请选择省!";
		} else if(city.equals("-1")) {
			errorInfo = "请选择市!";
		} else if(district.equals("-1")) {
			errorInfo = "请选择地区!";
		} else if(StringUtils.isEmpty(address)) {
			errorInfo = "请填写详细地址!";
		} else if(null==receiver || StringUtils.isEmpty(receiver.trim())) {
			errorInfo = "请填写收货人!";
		} else if(null==mobile || StringUtils.isEmpty(mobile)) {
			errorInfo = "请填写手机号码!";
		}
		return errorInfo;
	}

    @RequestMapping(value="trade/manual_sync_trade_form")
    public String manaualSyncTradeForm(Model model) {

        model.addAttribute("shopList", adminService.getSyncShopList());
        return "trade/manual_sync_trade_form";
    }

	@RequestMapping(value="trade/action/manual_sync_trade")
	public @ResponseBody ResultInfo syncTrade(@RequestParam("sellerNick") String sellerNick,
			@RequestParam("start") String start, @RequestParam("end") String end) throws IOException, ServletException, JSONException, ParseException, InsufficientStockException, InsufficientBalanceException {
		ResultInfo result = new ResultInfo();
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Date startDate = null;
		Date endDate = null;
		try {
			startDate = format.parse(start);
			endDate = format.parse(end);
			if(startDate.after(endDate)) {
				result.setSuccess(false);
				result.setErrorInfo("结束时间不能早于开始时间!");
				return result;
			}
		} catch(Exception e) {
			log.error("Error while sync trade", e);
			result.setSuccess(false);
			result.setErrorInfo("请输入正确的时间格式!");
			return result;
		}
		
		int count = 0;
		try {
			count = initTrades(Arrays.asList(topConfig.getToken(sellerNick)), startDate, endDate);
		} catch (Exception e) {
			log.error("Error while sync trade", e);
			result.setSuccess(false);
			result.setErrorInfo("系统异常，部分订单同步失败");
			return result;
		}
		log.info("Success sync trade {}", count);
		result.setErrorInfo("成功导入" + count + "条交易信息");
		return result;
	}

    public int initTrades(List<String> tokenList, Date startDate, Date endDate) throws ApiException, ParseException {
        int totalCount = 0;

        List<Trade> tradeList = tradeClient.queryTradeList(tokenList, startDate, endDate);
        for(Trade trade : tradeList) {
            //check trade if exists
            MyTrade dbMyTrade = tradeMapper.selectByTid(String.valueOf(trade.getTid()));
            Trade tradeDetail = tradeClient.getTradeFullInfo(trade.getTid(), topConfig.getToken(trade.getSellerNick()));
            if(null == tradeDetail) {
                continue;
            }
            MyTrade myTrade = tradeService.toMyTrade(tradeDetail);
            if(null == myTrade)
                continue;
            if(null == dbMyTrade) {
                myTrade.setStatus(TradeStatus.UnSubmit.getStatus());
                int count = tradeService.insertMyTrade(myTrade, false, Constants.LOCK_QUANTITY, null);
                totalCount += count;
            }
        }
        return totalCount;
    }

    @RequestMapping(value="trade/action/modify_receiver_info_form")
    public String modifyReceiverInfoForm(Model model, @RequestParam("id") String id, @RequestParam(required = false) String from) {

        MyTrade trade = tradeMapper.selectTradeMap(id);
        model.addAttribute("logistics", myLogisticsCompanyMapper.list());
        model.addAttribute("trade", trade);
        model.addAttribute("from", from);
        return "trade/modify_receiver_info_form";
    }

    @RequestMapping(value="trade/action/modify_receiver_info")
    public String modifyReceiverInfo(Model model, HttpServletRequest req, HttpServletResponse resp) {

        String province = req.getParameter("province");
        String city = req.getParameter("city");
        String district = req.getParameter("district");
        String address = req.getParameter("address");
        String receiver = req.getParameter("receiver");
        String phone = req.getParameter("phone");
        String mobile = req.getParameter("mobile");
        String id = req.getParameter("id");
        String from = req.getParameter("from");
        String errorInfo = validateAddress(model, province, city, district, address, receiver, mobile);
        if(null != errorInfo) {
            model.addAttribute("errorInfo", errorInfo);
            return "error_page";
        }
        String postcode = req.getParameter("postcode");

        String provinceName = (String) redisTemplate.opsForValue().get(REDIS_AREA + ":" + province);
        if(null == provinceName) {
            provinceName = areaMapper.selectById(Long.parseLong(province)).getName();
            redisTemplate.opsForValue().set(REDIS_AREA + ":" + province, provinceName);
        }

        String cityName = (String) redisTemplate.opsForValue().get(REDIS_AREA + ":" + city);
        if(null == cityName) {
            cityName = areaMapper.selectById(Long.parseLong(city)).getName();
            redisTemplate.opsForValue().set(REDIS_AREA + ":" + city, cityName);
        }

        String districtName = (String) redisTemplate.opsForValue().get(REDIS_AREA + ":" + district);
        if(null == districtName) {
            districtName = areaMapper.selectById(Long.parseLong(district)).getName();
            redisTemplate.opsForValue().set(REDIS_AREA + ":" + district, districtName);
        }
        int count = tradeMapper.updateLogisticsAddress(provinceName, cityName, districtName, address, mobile, phone,
                postcode, receiver, new Date(), id);
        if(count > 0) {
            try {
                if("list".equals(from)) {
                    resp.sendRedirect("/trade/trade_list?isCancel=0&isSubmit=0&isFinish=0&map=true");
                } else {
                    resp.sendRedirect("/trade/trade_detail?id=" + id);
                }
                return null;
            } catch (IOException e) {
            }
        } else {
            model.addAttribute("info", "修改收货地址失败!");
        }
        return "success_page";
    }

}
