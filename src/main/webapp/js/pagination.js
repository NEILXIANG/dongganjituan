$(document).ready(function(){
		
	  initpage();
      $('.pagination').jqPagination({
		    paged: function(page) {
		    	var status = $('#status').val();
		    	var distributor = $('#distributor').val();
		    	if(!distributor) {
		    		distributor='';
		    	}
		    	var start = $('#start').datetimebox('getValue');
                var end = $('#end').datetimebox('getValue');
                if(start.length==0 || end.length==0) {
                    start = "";
                    end = "";
                }
		    	var seller_nick = $('#seller_nick').val();
		    	var name = $('#name').val();
		    	var tid = $('#tid').val();
		    	var delivery = $('#delivery').val();
		    	var isSubmit = $('#isSubmit').val();
		    	var isCancel = $('#isCancel').val();
		    	var isFinish = $('#isFinish').val();
		    	var isRefund = $('#isRefund').val();
		    	var isSend = $('#isSend').val();
		    	if(!page) {
		    	    page = 1;
		    	}
		        window.location.href="/trade/trade_list?page=" + page + "&status=" + status + "&seller_nick=" + seller_nick
		        	+ "&name=" + name + "&tid=" + tid + "&dId=" + distributor + "&delivery=" + delivery + "&start=" + start
                    + "&end=" + end + "&isSubmit=" + isSubmit + "&isCancel=" + isCancel + "&isFinish=" + isFinish + "&isRefund=" + isRefund
                    + "&isSend=" + isSend;
		    }
	   });
	   
});

 function strDateTime(str) {
    var reg = /^(\d{1,4})(-|\/)(\d{1,2})\2(\d{1,2}) (\d{1,2}):(\d{1,2}):(\d{1,2})$/;
    var r = str.match(reg);
    if(r==null)return false;
    var d= new Date(r[1], r[3]-1,r[4],r[5],r[6],r[7]);
    return (d.getFullYear()==r[1]&&(d.getMonth()+1)==r[3]&&d.getDate()==r[4]&&d.getHours()==r[5]&&d.getMinutes()==r[6]&&d.getSeconds()==r[7]);
  }

//$('.pagination').jqPagination({
//		
//		max_page	: 40,
//		paged		: function(page) {
//			$('.log').prepend('<li>Requested page ' + page + '</li>');
//		}
//	});
