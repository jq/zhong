

(function($) {
	$(function() {
		$("#search").submit(function(event, info) {
			var div_search_result = $("#search_result");
			
			var text = $("input[id=search-text]", this).attr("value");
			if (text==null || text.length==0) {
				confirm("The search key word should not be empty.");
				return;
			}
			jQT.goTo(div_search_result);
//			text.blur();
			var results = $("#results", div_search_result).empty();
			var load_more = "Tap to load more";
			var url = "/ringtoneserver/search?q="+text;

			$(".loader", div_search_result).bind("ajaxSend", function(){
				var length = $(".store", div_search_result).length;
				if (length == 0) {
					$("ul" ,div_search_result).hide();
					$(this).show();
				}
			}).bind("ajaxComplete", function(){
				$("ul" ,div_search_result).show();
				$(this).hide();
			});
			
			$.getJSON(url, 
				function(data) {
				if (data.length == 0) {
					window.alert("No result");
					return;
				}
				$.each(data, function(i, item) {
					var li = jsonToListItem(item);
					li.appendTo(results);
				});
				
				$("<li id=\"load_more\">").text(load_more).appendTo(results);
				$("#load_more", div_search_result).click(function() {
//					jQT.goTo("#categories", 'slide');
					append_more(div_search_result, url);
				});
				$("#load_more", div_search_result).bind("ajaxSend", function(){
					$(this).text("Loading...");
				 }).bind("ajaxComplete", function(){
					 $(this).text("Tap to load more");
					 $(this).appendTo(results);
				 });
				
			});
			

			return false;
		});
	});
})(jQuery);

//$(function(){
//    $(".song_item").click( function(e) {
//    	var link = $(this).attr("href");
//    	//window.location = link;
//    	window.open(link);
//    });
//});

var last_email;
//rate is "stars4" which to be set as class, etc...
function fill_details_page(artist, title, rate, download_count, download_link, img_link, uuid) {
	var div_details = $("#details");
	clearRate();
	if (last_email!=null) {
		$("#email_text", div_details).attr("value", last_email);
	}
	$("audio", div_details).remove();
	$("#s3_image", div_details).attr("src", img_link);
	$(".artist", div_details).text(artist);
	$(".title", div_details).text(title);
	$(".download_count", div_details).text("Downloaded "+download_count+" times");
	$("#rate_star", div_details).attr("class", rate);
	$("#uuid", div_details).attr("value", uuid);
	var audio = $("<audio>").attr("src", download_link).attr("class", "player").attr("controls", "controls");
	var div_music_info_a = $("a", ".music_info", div_details);
	audio.appendTo(div_music_info_a);
//	$("audio", div_details).attr("src", download_link);
}


function fill_cate_page(cate, cate_name) {
	var div_cate = $("#category");
	$("h1", div_cate).text(cate_name);
	var results = $("#results", div_cate);
	results.empty();
	var url = "/ringtoneserver/search?q="+cate+"&type=category";
	
	$(".loader", div_cate).bind("ajaxSend", function(){
		var length = $(".store", div_cate).length;
		if (length == 0) {
			$("ul" ,div_cate).hide();
			$(this).show();
		}
	}).bind("ajaxComplete", function(){
		$(this).hide();
		$("ul" ,div_cate).show();
	});
	
	$.getJSON(url, 
			function(data) {
		if (data.length == 0) {
			window.alert("No result");
			return;
		}
			$.each(data, function(i, item) {
				var li = jsonToListItem(item);
				li.appendTo(results);
			});
			var load_more = "Tap to load more";
			$("<li id=\"load_more\">").text(load_more).appendTo(results);
			$("#load_more", div_cate).click(function() {
				append_more(div_cate, url);
			});
			$("#load_more", div_cate).bind("ajaxSend", function(){
				$(this).text("Loading...");
			 }).bind("ajaxComplete", function(){
				 $(this).text("Tap to load more");
				 $(this).appendTo(results);
				 setHeight($(this));
			 });
		});
};

//$(document).ready(function(e){
//        	var all_categories = $("#all_categories", this).empty();
//        	var all_cate_array = [ "Christian", "Metal", "Holiday", "R&B", "World Music",
//					"Pop", "Rock", "Games", "Dance", "Rap", "Jazz", "Hip-Hop",
//					"Gospel", "TV", "Hard Rock", "Electronic", "Latin Music",
//					"Blues", "Sound Effects", "Classical", "Comedy", "Country",
//					"Movie", "Other", "Hip", "Vocal", "Folk", "Hard", "Gothic",
//					"Avantgarde", "unknown", "Acid", "Soundtrack", "Soul", 
//					"Progressive", "Acoustic", "Ska", "Booty", "Easy", "Satire",
//					"Gangsta", "Oldies", "Heavy", "Southern", "Classic", "Disco",
//					"Alt", "Reggae", "Funk" ];
//        	for (cate in all_cate_array) {
//        		var a = $("<a></a>").attr("class", "cate_link_item").attr("href", "#category").attr("value", all_cate_array[cate]).text(all_cate_array[cate]);
//        		var str = $("<li></li>").attr("class", "arrow").append(a);
//        		str.appendTo(all_categories);
//        	}
//});

$(document).ready(function(e){
    $('#top_download').bind('pageAnimationStart', function(event, info) {
        if (info.direction == 'in') {
        	var div_top_download = this;
        	var item_count = $(".store", this).length;
        	if (item_count > 0) {
        		return;
        	}
			var results = $("#results", this).empty();
			var load_more = "Tap to load more";
			var url = "/ringtoneserver/search?type=download_count";
			
			$(".loader", this).bind("ajaxSend", function(){
				var length = $(".store", "#top_download").length;
				if (length == 0) {
					$("ul" , "#top_download").hide();
					$(this).show();
				}
			}).bind("ajaxComplete", function(){
				$("ul" , "#top_download").show();
				$(this).hide();
			});
			
			$.getJSON(url, 
				function(data) {
				if (data.length == 0) {
					window.alert("No result");
					return;
				}
				$.each(data, function(i, item) {
					var li = jsonToListItem(item);
					li.appendTo(results);
				});
				$("<li id=\"load_more\">", div_top_download).text(load_more).appendTo(results);
				$("#load_more", $("#top_download")).click(function() {
					append_more($("#top_download"), url);
				});
				$("#load_more", $("#top_download")).bind("ajaxSend", function(){
					$(this).text("Loading...");
				 }).bind("ajaxComplete", function(){
					$(this).text("Tap to load more");
					$(this).appendTo(results);
				 });
			});
        }
    })
});

$(document).ready(function(e){
	$("#details").bind('pageAnimationStart', function(event, info) {
		if (info.direction == 'out') {
			$("audio", this).remove();
		}
	});
});


$(document).ready(function(e){
    $('#newest_add').bind('pageAnimationStart', function(event, info) {
        if (info.direction == 'in') {
        	var div_newest_add = this;
        	var item_count = $(".store", this).length;
        	if (item_count > 0) {
        		return;
        	}
			var results = $("#results", this).empty();
			var load_more = "Tap to load more";
			var url = "/ringtoneserver/search?type=add_date";
			
			$(".loader", this).bind("ajaxSend", function(){
				var length = $(".store", "#newest_add").length;
				if (length == 0) {
					$("ul" , "#newest_add").hide();
					$(this).show();
				}
			}).bind("ajaxComplete", function(){
				$("ul" , "#newest_add").show();
				$(this).hide();
			});
			
			$.getJSON(url, 
				function(data) {
				if (data.length == 0) {
					window.alert("No result");
					return;
				}
				$.each(data, function(i, item) {
					var li = jsonToListItem(item);
					li.appendTo(results);
				});
				$("<li id=\"load_more\">", div_newest_add).text(load_more).appendTo(results);
				$("#load_more", $("#newest_add")).click(function() {
					append_more($("#newest_add"), url);
				});
				$("#load_more", $("#newest_add")).bind("ajaxSend", function(){
					$(this).text("Loading...");
				 }).bind("ajaxComplete", function(){
					$(this).text("Tap to load more");
					$(this).appendTo(results);
				 });
			});
        }
    })
});




function append_more(parent, url) {
	var results = $("#results", parent);
	var loading = $("#load_more", parent);
	var start = $(".store", parent).length;
	$.getJSON(url+"&start="+start,
		function(data) {
		$.each(data, function(i, item) {
			var str = jsonToListItem(item);
			str.appendTo(results);
		});
		if ($('.store', parent).length > 50) {
			loading.hide();
		}
	});
};

function jsonToListItem(item) {
	var arrow = $("<span></span>").attr("class", "arrow");
	var artist = $("<span></span>").attr("class", "artist").attr("value", item.artist).text(item.artist);
	var title = $("<span></span>").attr("class", "title").attr("value", item.title).text(item.title);
	var rate;
	if (item.avg_rate == 0) {
		rate = $("<span></span>").attr("class", "stars0").attr("value", item.avg_rate);
	} else if (item.avg_rate <= 20) {
		rate = $("<span></span>").attr("class", "stars1").attr("value", item.avg_rate);
	} else if (item.avg_rate <= 40) {
	 	rate = $("<span></span>").attr("class", "stars2").attr("value", item.avg_rate);
	} else if (item.avg_rate <= 60) {
		rate = $("<span></span>").attr("class", "stars3").attr("value", item.avg_rate);
	} else if (item.avg_rate <= 90) {
		rate = $("<span></span>").attr("class", "stars4").attr("value", item.avg_rate);
	} else if (item.avg_rate <= 100) {
		rate = $("<span></span>").attr("class", "stars5").attr("value", item.avg_rate);
	}
	var download_count = $("<span></span>").attr("style", "display:none;").attr("class", "download_count").attr("value", item.download_count).text(item.down);
//	var image = $("<span></span>").attr("class", "image").attr("style", "background-image: url('"+item.image+"'); background-position:center; background-repeat:no-repeat;").attr("value", item.image);
	var loading_img = $("<img></img>").attr("src", "/images/spinner.gif");
	var image = $("<img></img>").attr("src", item.image).hide();
	image.load(function(e) {
		loading_img.hide();
		image.fadeIn();
	});
	image.error(function(e) {
		loading_img.attr("src", "/images/load_failed.png");
	});
//	var a = $("<a></a>").attr("href", item.s3url).attr("target", "_blank").attr("class", "song_item").attr("song_id", item.uuid);
	var onclick = "javascript:fill_details_page(\""+item.artist+"\",\""+item.title+"\",\""+rate.attr("class")+"\",\""+item.download_count+"\",\""+item.s3url+"\",\""+item.image+"\",\""+item.uuid+"\");";
	var a = $("<a></a>").attr("href", "#details").attr("class", "song_item").attr("onClick", onclick).attr("song_id", item.uuid).attr("value", item.s3url);
	var li = $("<li></li>").attr("class", "store");
	var div_a = a.append(loading_img).append(image).append(artist).append(title).append(rate).append(download_count).append(arrow);
	return li.append(div_a);
};

function sendEmail(email, link) {
	if (!isEmail(email)) {
		confirm("The format is not correct.");
		return;
	} else {
		lastEmail = email;
		var url = "/ringtoneserver/sendemail?download_link="+link+"&email="+email;
		$.get(url,
			function(data){
				if (data == "ok") {
					confirm("The email will be send in several minutes, please download the ringtone from your computer and read the instructions which will tell you how to install the ringtone with iTunes.");
				} else {
					confirm("The request for sending email failed. Please try again.");
				}
		});
	}
};

function isEmail(strEmail) {
	if (strEmail.search(/^\w+((-\w+)|(\.\w+))*\@[A-Za-z0-9]+((\.|-)[A-Za-z0-9]+)*\.[A-Za-z0-9]+$/) != -1) {
		return true;
	}
	else {
		return false;
	}
};

function clearRate() {
	for (i=1; i<=5; i++) {
		$("#star"+i).html("<img src='/images/empty_star.png'></img>");
	}
}

function doRate(num) {
	for (i=1; i<=num; i++) {
		$("#star"+i).html("<img src='/images/full_star.png'></img>");
	}
	for (i=num+1; i<=5; i++) {
		$("#star"+i).html("<img src='/images/empty_star.png'></img>")
	}
	var uuid = $("#uuid").attr("value");
	var rate_url = "/ringtoneserver/rate?uuid="+uuid+"&rate="+num*20
	$.get(rate_url,
		function(data){

		});
};