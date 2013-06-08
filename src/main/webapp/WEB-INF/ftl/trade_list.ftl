<#import "/templates/root.ftl" as root >

<script language="javascript" src="${rc.contextPath}/js/LodopFuncs.js"></script>
<object  id="LODOP" classid="clsid:2105C259-1E0C-4534-8141-A753534CB4CA" width=0 height=0> 
    <embed id="LODOP_EM" type="application/x-print-lodop" width=0 height=0></embed>
</object>

<@root.html active=3 css=["trade_list.css", "jqpagination.css"] js=["pagination.js", "jquery.jqpagination.min.js"]>
	<i class="icon-list-alt"></i>订单列表
	<div style="width: 100%; height: 30px; background-color: #99CCCC; padding-top: 5px; padding-left: 20px;">
		<strong>状态</strong>
		<select id="status" style="width: 8%;">
			<option value="">所有状态</option>
			<option value="0">新建</option>
			<option value="1">待审核</option>
			<option value="2">待发货</option>
			<option value="3">拣货中</option>
			<option value="4">已发货</option>
			<option value="5">已完成</option>
			<option value="-1">已作废</option>
			<option value="-3">已退货</option>
		</select>
		&nbsp;&nbsp;&nbsp;&nbsp;
		<strong>分销商</strong>
		<select id="seller_nick" style="width: 8%;">
			<option value="">所有分销商</option>
			<option value="志东张">志东张</option>
			<option value="小花生米">小花生米</option>
		</select>
		&nbsp;&nbsp;&nbsp;&nbsp;
		<strong>收货人姓名</strong>
		<input id="name" type="input" value="" style="width: 6%; height: 20px;"/>
		&nbsp;&nbsp;&nbsp;&nbsp;
		<button id="search">搜索</button>
	</div>
	<div class="pagination">
	    <a href="#" class="first" data-action="first">&laquo;</a>
	    <a href="#" class="previous" data-action="previous">&lsaquo;</a>
	    <input id="page" type="text" readonly="readonly" data-current-page="${paging.page}" data-page-string="1 of 3" data-max-page="${paging.pageCount}" />
	    <a href="#" class="next" data-action="next">&rsaquo;</a>
	    <a href="#" class="last" data-action="last">&raquo;</a>
	</div>
	<#list trade_list as trade>
		<table>
		    <tbody>
		      <tr class="trade">
		        <td colspan="7">
		        	${trade.come_from!}&nbsp;&nbsp;&nbsp;
		        	<strong>订单编号：</strong></strong>${trade.tid?c}  &nbsp;&nbsp;&nbsp;
		        	<strong>创建时间：</strong>${trade.created?string('yyyy-MM-dd HH:mm:ss')} &nbsp;&nbsp;&nbsp;
		        	<strong>卖家: </strong>${trade.seller_nick} &nbsp;&nbsp;&nbsp;
		        	<strong>买家: <strong>${trade.name} &nbsp;&nbsp;&nbsp;
		        	<strong>${trade.oStatus.desc}<strong>
		        </td>
		      </tr>
		      <#assign orderList = trade.myOrderList>
		      <#list orderList as order>
		      	  <tr class="order_list">
		      	  		<td  style="width: 80px;">
		      	  			<img style="width: 80px; height: 80px;" src="${order.pic_path}_80x80.jpg" />
		      	  		</td>
				        <td style="width: 25%;">
				        	<p><strong>名称：</strong>${order.title}</p>
				        	<p><strong>编号：</strong>${order.goods_id}</p>
				        	<p><strong>属性：</strong>${order.sku.color} &nbsp;&nbsp; ${order.sku.size}</p>
				        </td>
				        <td style="width: 10%;">
				        	<p><strong>${order.oStatus.desc}</strong></p>
				        	<p><strong>数量：</strong>${order.quantity}</p>
				        	<p><strong>价格：</strong>${order.payment/100}元</p>
				        </td>
				        <#if order_index==0>
				        <td rowspan="${orderList?size}" style="width: 25%;">
				        	<p><strong>地址：</strong>${trade.state}${trade.city}${trade.district}${trade.address}</p>
				        	<p>
				        		<strong>收货人：</strong>${trade.name} &nbsp;&nbsp;
				        		<strong>电话：</strong>${trade.phone} &nbsp;&nbsp;
				        		<strong>手机：</strong>${trade.mobile} &nbsp;&nbsp;
				        	</p>
				        	<p><strong>邮编：</strong>${trade.postcode}</p>
				        	<#if trade.status=="WAIT_BUYER_CONFIRM_GOODS">
				        		<p><strong>快递：</strong>${order.logistics_company} &nbsp;&nbsp;<strong>单号：</strong>${order.invoice_no}</p>
				        	</#if>
				        	<p>
				        		<strong>总数：</strong>${trade.goods_count} &nbsp;&nbsp;
				        		<strong>总额：</strong>${trade.payment/100}元 &nbsp;&nbsp;
				        		<strong>快递费：</strong>${trade.delivery_money/100}元
				        	</p>
				        </td>
				        <td rowspan="${orderList?size}" style="width: 15%;">
				        	<p><strong>买家留言：</strong>${trade.buyer_message!}</p> &nbsp;&nbsp;
				        	<p><strong>备注：</strong>${trade.seller_memo!}</p>
				        </td>
				        <td rowspan="${orderList?size}">
				        	<#if trade.status=="WAIT_SELLER_SEND_GOODS">
				        		<p>快递：
				        			<select id="logistics" style="">
				        				<#list logistics as lo>
				        					<option value="${lo.code}" <#if trade.delivery==lo.name>selected</#if>>${lo.name}</option>
				        				</#list>
									</select>
				        		</p>
				        		<p>发货</p>
				        		<p>打印快递单 &nbsp;&nbsp; <a href="javascript:prn1_preview()">预览</a></p>
				        		<p>打印发货单 &nbsp;&nbsp; 预览</p>
				  
				        	</#if>
				        </td>
				        </#if>
			      </tr>
		      </#list>
		    </tbody>
		</table>
	</#list>
	<div class="pagination" style="float: right;">
	    <a href="#" class="first" data-action="first">&laquo;</a>
	    <a href="#" class="previous" data-action="previous">&lsaquo;</a>
	    <input type="text" readonly="readonly" data-current-page="${paging.page}" data-page-string="1 of 3" data-max-page="${paging.pageCount}" />
	    <a href="#" class="next" data-action="next">&rsaquo;</a>
	    <a href="#" class="last" data-action="last">&raquo;</a>
	</div>
	
</@root.html>


<script>
	function initpage() {
	      $('#name').val('${name!""}');
	      $('#seller_nick').val('${seller_nick!""}');
	      $('#status').val('${status!""}');
	}
	
    var LODOP; //声明为全局变量 
	function prn1_preview() {	
		CreateOneFormPage();	
		LODOP.ADD_PRINT_SETUP_BKIMG("<img border='0' src='${rc.contextPath}/img/kuaidi/shentong.jpg'>");
		LODOP.SET_SHOW_MODE("BKIMG_IN_PREVIEW",1); //注："BKIMG_IN_PREVIEW"-预览包含背景图 "BKIMG_IN_FIRSTPAGE"- 仅首页包含背景图
		LODOP.SET_PRINT_STYLE("FontSize",18);
		LODOP.SET_PRINT_STYLE("Bold",1);
		LODOP.ADD_PRINT_TEXT(110,86,100,25,"寄件人");
		LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
		LODOP.ADD_PRINT_TEXT(109,253,111,26,"始发地");
		LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
		LODOP.ADD_PRINT_TEXT(150,87,260,25,"发件单位名称");
		LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
		LODOP.ADD_PRINT_TEXT(187,79,302,62,"发货地址");
		LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
		LODOP.ADD_PRINT_TEXT(259,103,164,25,"联系电话");
		LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
		LODOP.ADD_PRINT_TEXT(112,484,111,25,"收货人");
		LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
		LODOP.ADD_PRINT_TEXT(147,479,249,27,"收件单位名称");
		LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
		LODOP.ADD_PRINT_TEXT(189,470,309,56,"收件地址");
		LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
		LODOP.ADD_PRINT_TEXT(261,506,157,24,"联系电话");
		LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
		LODOP.ADD_PRINT_TEXT(112,649,125,26,"目的地");
		LODOP.SET_PRINT_STYLEA(0,"FontSize",10);
		//LODOP.PRINT_DESIGN();
		LODOP.PREVIEW();	
	};
	function prn1_print() {		
		CreateOneFormPage();
		LODOP.PRINT();	
	};
	function prn1_printA() {		
		CreateOneFormPage();
		LODOP.PRINTA(); 	
	};	
	function CreateOneFormPage(){
		LODOP=getLodop(document.getElementById('LODOP'),document.getElementById('LODOP_EM'));  
		LODOP.PRINT_INIT("打印控件功能演示_Lodop功能_表单一");
		LODOP.SET_PRINT_STYLE("FontSize",18);
		LODOP.SET_PRINT_STYLE("Bold",1);
		//LODOP.ADD_PRINT_TEXT(50,231,260,39,"打印页面部分内容");
		//LODOP.ADD_PRINT_HTM(88,200,350,600,document.getElementById("form1").innerHTML);
	};	                     
	function prn2_preview() {	
		CreateTwoFormPage();	
		LODOP.PREVIEW();	
	};
	function prn2_manage() {	
		CreateTwoFormPage();
		LODOP.PRINT_SETUP();	
	};	
	function CreateTwoFormPage(){
		LODOP=getLodop(document.getElementById('LODOP'),document.getElementById('LODOP_EM'));  
		LODOP.PRINT_INIT("打印控件功能演示_Lodop功能_表单二");
		LODOP.ADD_PRINT_RECT(70,27,634,242,0,1);
		LODOP.ADD_PRINT_TEXT(29,236,279,38,"页面内容改变布局打印");
		LODOP.SET_PRINT_STYLEA(2,"FontSize",18);
		LODOP.SET_PRINT_STYLEA(2,"Bold",1);
		LODOP.ADD_PRINT_HTM(88,40,321,185,document.getElementById("form1").innerHTML);
		LODOP.ADD_PRINT_HTM(87,355,285,187,document.getElementById("form2").innerHTML);
		LODOP.ADD_PRINT_TEXT(319,58,500,30,"注：其中《表单一》按显示大小，《表单二》在程序控制宽度(285px)内自适应调整");
	};              
	function prn3_preview(){
		LODOP=getLodop(document.getElementById('LODOP'),document.getElementById('LODOP_EM'));  
		LODOP.PRINT_INIT("打印控件功能演示_Lodop功能_全页");
		LODOP.ADD_PRINT_HTM(20,40,700,900,document.documentElement.innerHTML);
		LODOP.PREVIEW();	
	};	
</script>
