// v1.0.1
var version = "ΚΑΡΠΑΘΟΣ planner - 2023 by ΓΕΩΡΓΙΟΣ ΛΕΟΥ ΟΘΟΣ ";
//////
//////
//////
//////
window.onload = function() {
    if (window.jQuery) {
        // jQuery is loaded
        // alert("Yeah!");
    } else {
        // jQuery is not loaded
        alert("jQuery Doesn't Work");
    }
}// window.onload



// function delAction(id) {


//     w2confirm(
//     'Delete Cancelled   '+ id +'  Are you sure?')
//         .yes(() => {
//             delActionAjaxCall(id)
//         })
//         .no(() => {
//             w2alert('Nothing changed or deleted ');
//         })
// }

// function  delActionAjaxCall(id){
//  // console.log('Yes')

//             // String delUrl = "/booking/del?bid=" + booking.getId();
//             $.ajax({
//                 type: 'GET',
//                 url: '/booking/del',
//                 data: {
//                     bid: id
//                 },
//                 contentType: 'application/json',
//                 success: function (data, status, xhr) {
//                     w2alert('Deleted a cancelled booking - all good re-run the report to confirm');
//                 }
//             });  // ajax

// }



// functions
function updateClock (){
 	var currentTime = new Date ( );
  	var currentHours = currentTime.getHours ( );
  	var currentMinutes = currentTime.getMinutes ( );
  	var currentSeconds = currentTime.getSeconds ( );

  	// Pad the minutes and seconds with leading zeros, if required
  	currentMinutes = ( currentMinutes < 10 ? "0" : "" ) + currentMinutes;
  	currentSeconds = ( currentSeconds < 10 ? "0" : "" ) + currentSeconds;

  	// Choose either "AM" or "PM" as appropriate
  	var timeOfDay = ( currentHours < 12 ) ? "AM" : "PM";

  	// Convert the hours component to 12-hour format if needed
  	currentHours = ( currentHours > 12 ) ? currentHours - 12 : currentHours;

  	// Convert an hours component of "0" to "12"
  	currentHours = ( currentHours == 0 ) ? 12 : currentHours;

  	// Compose the string for display
  	var currentTimeString = currentHours + ":" + currentMinutes + ":" + currentSeconds + " " + timeOfDay;


   	$("#clock").html(currentTimeString);
 }






// function financial(x) {
//     if (x) {
//         return Number.parseFloat(x).toFixed(2);
//         // .replace('.', ',');
//     }
//     return Number.parseFloat(0).toFixed(2);
//     // .replace('.', ',');
// }


function convertDate(str) {
    var parts = str.split("/");
    var dt = new Date(parseInt(parts[2], 10), parseInt(parts[1], 10) - 1, parseInt(parts[0], 10));
    dt.setHours(0, 0, 0);

    return dt;
}

// function convertDate_00_00_00(str) {
//     var parts = str.split("/");
//     var dt = new Date(parseInt(parts[2], 10), parseInt(parts[1], 10) - 1, parseInt(parts[0], 10));
//     dt.setHours(0, 0, 0);

//     return dt;
// }


function initForm() {

     $('[name="save"]').hide();
                        $('[name="reset"]').hide();
                        w2ui.myForm.hide('name', 'startDate', 'extraInfo');


    w2ui['myForm'].setValue('startDate', formattedDate(new Date));
    w2ui['myForm'].setValue('endDate', formattedDate(new Date));
    // w2ui['myForm'].setValue('numOfNights', 0);

    // w2ui['myForm'].setValue('status', { id: 'active', text: 'Active' });


    // w2ui['myForm'].setValue('balance', null);
    // w2ui['myForm'].setValue('charge', null);
    // w2ui['myForm'].setValue('received', null);
    // w2ui['myForm'].setValue('commission', null);
    w2ui['myForm'].refresh();




}
function formattedDate(d = new Date) {
    let month = String(d.getMonth() + 1);
    let day = String(d.getDate());
    const year = String(d.getFullYear());

    if (month.length < 2) month = '0' + month;
    if (day.length < 2) day = '0' + day;

    return `${day}/${month}/${year}`;
}

// vars
var nowISOString = new Date().toISOString().substring(0, 10);
var moveToDate = new Date();// set to current date
// initialise vis timeline componenet
var groups = new vis.DataSet();
// var items = new vis.DataSet();




var items = new vis.DataSet({type: { start: "ISODate", end: "ISODate" }});   
var timelineoOtions = {

    locale: 'en',

    maxHeight: 2000,
    horizontalScroll: true,
    verticalScroll: true,
    zoomKey: "ctrlKey",

    start: Date.now() - 1000 * 60 * 60 * 24 * 2, // minus 3 days
    end: Date.now() + 1000 * 60 * 60 * 24 * 17, // plus 1 months aprox.

    // zoomMin: 1000 * 60 * 60 * 24 * 16,             // 16  day in milliseconds
    // zoomMax: 1000 * 60 * 60 * 24 * 29 * 1,         // about 1  months in milliseconds

    showCurrentTime: true,

    clickToUse: false,

    tooltip: {
        followMouse: true,
    },

    stack: true,

    // editable: true,

    editable: {
        add: false,
        remove: true
    },


    // moment: function(date) {
    //     return  vis.moment(date).utc().utcOffset('+02:00');  // greek utc time
    // },

    orientation: {
        axis: "both",
        item: "top"
    },

    showMinorLabels: true,
    timeAxis: { scale: 'day', step: 1 },
    format: {
        minorLabels: {
            millisecond: '',
            second: '',
            minute: '',
            hour: '',
            weekday: '',
            day: 'DD ddd',
            month: '',
            year: ''
        }
    },


    onRemove: function (item, callback) {


        alert ( 'delete' );


        var pojo = JSON.stringify(items.get(item.id).recordData);

        w2confirm('Delete  '+ items.get(item.id).recordData.name +'    Are you sure?')
            .yes(() => {

                $.ajax({
                    type: 'POST',
                    url: '/booking/delete',
                    contentType: 'application/json',
                    data: pojo,
                    success: function (data, status, xhr) {
                        // alert('status: ' + status + ', data: ' + data);
                        callback(item); // do remove
                    }
                });

            })
            .no(() => {
                w2alert('Nothing changed or deleted ');
            })


    }

};
// add some rooms



var roomList = [


    {
            id: 'Dimos',
            text: '👮🏼 🏛️ Dimos/Government   www.karpathos.gr'
    },
    {
        id: 'Church',
        text: '⛪ Εκκλησία'
    },
    {
            id: 'ChurchHoliday',
            text: '⛪🎉 Πανηγύρι/Εορτή'
    },





];



// for the dropdown  select room
// roomList.push({id: idVal,  text: descVal});

//todo  check that the css ids ate updated
var agencyList = [
    { id: 'bookingcom', text: 'Πολιτιστικά/Cultural' },
    { id: 'other',      text: 'Αθλητικά/Sport' },
    { id: 'abnb',       text: 'Κοινωνικά/Social' },
    { id: 'walkin',     text: 'Συγκοινωνίες/Transportation' },
    { id: 'closed',     text: 'Dine/Wine' },
];


// var roomList = [

//     // 
//     {
//         id: 'Cultural',
//         text: 'Πολιτιστικά/Cultural'
//     },
  
//     {
//         id: 'Sport',
//         text: 'Αθλητικά/Sport'
//     },
//     {
//         id: 'Social',
//         text: '🛍️ Κοινωνικά/Social'
//     },
//     {
//         id: 'Transportation',
//         text: '🛍️ Συγκοινωνίες/Transportation'
//     }

// ];  // for the dropdown  select room


// var agencyList = [
//     { id: 'other',      text: 'default' },
// ];



// docOnReady
$(function () {

//////////////
// set globals
//////////////

            w2utils.settings.dateFormat = 'yyyy-mm-dd';
            
            // w2utils.settings.currencyPrefix = '€';
            $("#tid").html(""+ version );;
            $("#tid").css({ 'color': 'blue', 'font-size': '150%' });
            $("#tid").css('text-align', 'left');
            $("#clock").css('text-align', 'left');
            // CLOCK
            setInterval('updateClock()', 1000);

            var container = document.getElementById('visualization');
            timeline = new vis.Timeline(container, null, timelineoOtions);
            timeline.setGroups(groups);
            timeline.setItems(items);
            timeline.setCurrentTime(Date.now());

            for (let i = 0; i < roomList.length; i++) {

                var idVal = roomList[i].id;
                var descVal = roomList[i].text;
                groups.add({
                    id: idVal,
                    content: descVal,
                    order: i+1
                });
            }



            $("#hide").click(function () {
                    // $("#save").hide();
                    $('[name="save"]').hide();
                    $('[name="reset"]').hide();
                    w2ui.myForm.hide('name', 'startDate', 'extraInfo');

                });
            $("#show").click(function () {
                // $("#myForm").show();
                // $("#save").show();
                $('[name="save"]').show();
                $('[name="reset"]').show();
                w2ui.myForm.show('name', 'startDate', 'extraInfo')
            });
            $('#myForm').w2form({
                name: 'myForm',

                // header: 'Form with Toolbar',


                // toolbar: {
                //     items: [
                //         { id: 'bt1', type: 'button', text: 'Button 1', img: 'icon-folder' },
                //         { id: 'bt2', type: 'button', text: 'Button 2', img: 'icon-folder' },
                //         { id: 'bt3', type: 'spacer' },
                //         { id: 'bt4', type: 'button', text: 'GotoToday', img: 'icon-page' },
                //         { id: 'bt5', type: 'button', text: 'Save', img: 'icon-page' }
                //     ],
                //     onClick(event) {
                //         if (event.target == 'bt4') {

                //         }


                //         if (event.target == 'bt5') alert('save test');
                //     }
                // },


                // url      : {
                //     get  : '/server/url/to/get',
                //     save : '/booking/save'
                // },

                // need to set url  for save  and get
                fields: [


                    {
                        field: 'name', type: 'text',
                        required: true,
                        html: {
                            label: 'Name',
                            text: '&nbsp; &nbsp;   %anchorAgency%  &nbsp; &nbsp;   %anchorRoom%   ',
                            attr: 'style="  text-transform: uppercase; width: 300px; font-weight: bold; text-align:center; "'
                        }
                    },
                    
                    {
                        field: 'agency', type: 'list',
                        required: true,
                        html: {
                            label: '&nbsp; &nbsp;  Kατηγορία  &nbsp;  &nbsp; ',
                            anchor: '%anchorAgency%',
                            // text: '&nbsp;  %anchorVoucher% &nbsp; &nbsp;',
                            attr: 'style=" width: 200px;  font-weight: bold;  text-align:center;"'
                        },
                        options: { items: agencyList }
                    },

                    {
                        field: 'room', type: 'list',
                        required: true,
                        html: {
                            label: '&nbsp; &nbsp; Υπό-Kατηγορία &nbsp;',
                            anchor: '%anchorRoom%',
                            attr: 'style="  width: 300px; font-weight: bold; text-align:center; "'
                        },
                        options: { items: roomList }
                    },

                    // line
                    {
                        field: 'startDate', type: 'date',
                        required: true,
                        html: {
                            label: '&nbsp;&nbsp;&nbsp; Από ',
                            text: '&nbsp;  &nbsp;  &nbsp;    %anchorEndDate%  &nbsp;  &nbsp;',
                            attr: 'style="  width: 250px; font-weight: bold; text-align:center;"'
                        },
                        options: {
                            // start: new Date().toISOString().substring(0, 10) // nowISOString  e.g. '2023-02-25'
                            // end: query('input[type=endDate]')[0] ,
                            // start: formattedDate(new Date()),
                           format: 'dd/mm/yyyy'
                            // all dates before start will be unselectable
                        }
                    },



                    {

                        field: 'endDate', type: 'date',
                        required: true,
                        // on endDate change  calculate  numOfNights  from start date and endDate
                        html: {
                            label: '&nbsp;&nbsp;&nbsp; Εώς  &nbsp;&nbsp;&nbsp;&nbsp; ',
                            anchor: '%anchorEndDate%',
                            attr: 'style=" width: 250px; font-weight: bold; text-align:center;"'
                        },
                        options: {
                            // start: query('input[type=startDate]')[0]
                            // start: formattedDate(new Date()),
                            format: 'dd/mm/yyyy'
                        }
                    },

                 



                    // line
                    {
                        field: 'extraInfo', type: 'textarea',
                        html: {
                            label: 'Εxtra Info ',
                            attr: 'style="width: 600px; height: 60px; resize: none" '
                        }
                    },



                ],


                actions: {

                    // report: {

                    //     text: 'report',
                    //     class: 'w2ui-btn-blue',
                    //     style: 'text-transform: uppercase; width: 100px;  color: white ; font-size: 12px;',
                    //     onClick(event) {


                    //         $.ajax({
                    //             type: 'GET',
                    //             url: '/booking/report',
                    //             data: {
                    //                 fromDate: w2ui.myForm.getValue('startDate'),
                    //                 toDate: w2ui.myForm.getValue('endDate')
                    //             },
                    //             contentType: 'application/json',
                    //             success: function (data, status, xhr) {

                    //                 var contentHtml = data;

                    //                 w2popup.open({
                    //                     title: 'Report ',
                    //                     // with: 300,
                    //                     // height: 500,
                    //                     showMax: true,
                    //                     openMaximized: true,
                    //                     body: contentHtml,
                    //                     actions: { Ok: w2popup.close }
                    //                 });

                    //             }
                    //         });  // ajax


                    //     }
                    // },

                    // goto: {
                    //     text: 'goto',
                    //     class: 'w2ui-btn-blue',
                    //     style: 'text-transform: uppercase; width: 100px;  color: white ; font-size: 12px;',
                    //     onClick(event) {

                    //         var gotoDate = convertDate(w2ui.myForm.getValue('startDate')).setHours(0, 0, 0);
                    //         // moveToDate = gotoDate;

                    //         timeline.moveTo(gotoDate, { animation: true });

                    //     }
                    // },

                    today: {
                        text: 'Today',
                        class: 'w2ui-btn-blue',
                        style: 'text-transform: uppercase;  width: 100px; color: white; font-size: 12px; ',
                        onClick(event) {
                            moveToDate = new Date();
                            timeline.moveTo(moveToDate, { animation: true });
                        }
                    },


                    save: {
                        text: 'save',
                        class: 'w2ui-btn-green',
                        style: 'text-transform: uppercase; color: white; width: 200px;  font-size: 12px; ',
                        onClick(event) {

                            if (w2ui.myForm.validate().length == 0) {


                                // retrieve a filtered subset of the data
                                var fItems = items.get({
                                    filter: function (item) {
                                        

                                        var a_start = convertDate(w2ui.myForm.getValue('startDate'))  .setHours(0, 0, 0) ;
                                        var a_end = convertDate(w2ui.myForm.getValue('endDate'))  .setHours(23, 59, 59); ;

                                        var b_start = item.start;
                                        var b_end = item.end;



                                        // if (w2ui.myForm.getValue('room').id == item.group) {
                                        //     if (a_start <= b_start && b_start < a_end) return true;    // b starts in a
                                        //     if (a_start < b_end && b_end < a_end) return true;       // b ends in a
                                        //     if (b_start < a_start && a_end <= b_end) return true;     // a in b
                                        // }

                                        
                                        return false;

                                    }
                                });

                                if (fItems != null && fItems.length > 0 ) {

                                    w2popup.open({
                                        title: 'Message',
                                        with: 300,
                                        height: 150,
                                        body: ' 🚗|🛵  Object  is NOT available',
                                        actions: { Ok: w2popup.close }
                                    });

                                }
                                else {
                                    // do save



                                    // var newId = JSON.stringify(w2ui.myForm.getValue('room').id + '_' + w2ui.myForm.getValue('startDate') + '_' + w2ui.myForm.getValue('endDate'))

                                    var newItem = {
                                        id: null,
                                        // type: 'range',
                                        group: w2ui.myForm.getValue('room').id,
                                        // start: w2ui.myForm.getValue('startDate'),
                                        // end: w2ui.myForm.getValue('endDate'),
                                        start: convertDate(w2ui.myForm.getValue('startDate')).setHours(0, 0, 0),
                                        end: convertDate(w2ui.myForm.getValue('endDate')).setHours(23, 59, 59),

                                        content:  w2ui.myForm.getValue('name'),

                                        className: w2ui.myForm.getValue('agency').id,
                                        recordData: this.getCleanRecord(),
                                    };




                                    // alert(JSON.stringify(items.get(newId).recordData, null, 4));
                                    // start: new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0),
                                    // end: new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59),
                                    // this.save();// todo

                                    timeline.moveTo(convertDate(w2ui.myForm.getValue('startDate')).setHours(12, 0, 0), { animation: true });

                                    var pojo = JSON.stringify(newItem.recordData);

                                    $.ajax({
                                        type: 'POST',
                                        url: '/booking/save',
                                        contentType: 'application/json',
                                        data: pojo,
                                        success: function (element, status, xhr) {


                                                items.add({
                                                    id: element.id,
                                                    type: 'range',
                                                    group: element.room,
                                                    // start: element.startDate,
                                                    // end: element.endDate,

                                                    start: convertDate(element.startDate).setHours(0, 0, 0),
                                                    end: convertDate(element.endDate).setHours(23, 59, 59),

                                                    // title: element.title,
                                                    content: element.name,

                                                    className: element.agency,
                                                    recordData: element,
                                                });


                                        }
                                    });

                                }// else


                            } else {
                                w2alert("Fix problems then do save");
                            }



                        }
                    },


                    reset: {
                        text: 'reset',
                        class: 'w2ui-btn-red',
                        style: 'text-transform: uppercase; color: blue; font-size: 12px;',
                        onClick(event) {

                            this.clear();
                            timeline.setSelection([]); // unselect timeline item
                            initForm();

                        }
                    },


                    left90: {
                        text: '<< 90d',
                        class: 'w2ui-btn-grey',
                        style: 'text-transform: uppercase; color: black;  font-size: 12px; ',  // border: 1px solid black;
                        onClick(event) {
                            // w2alert('Custom Action')
                            moveToDate = new Date(moveToDate.getTime() - (90 * 24 * 60 * 60 * 1000));     /// plus 90 days
                            timeline.moveTo(moveToDate, { animation: true });
                        }
                    },

                    left30: {
                        text: '< 30d',
                        class: 'w2ui-btn-grey',
                        style: 'text-transform: uppercase;  color: black; font-size: 12px;  ',
                        onClick(event) {
                            // w2alert('Custom Action')
                            moveToDate = new Date(moveToDate.getTime() - (30 * 24 * 60 * 60 * 1000));     /// plus 300 days
                            timeline.moveTo(moveToDate, { animation: true });
                        }
                    },



                    right30: {
                        text: '30d >',
                        class: 'w2ui-btn-grey',
                        style: 'text-transform: uppercase;  color: black; font-size: 12px; ',
                        onClick(event) {
                            // w2alert('Custom Action')
                            moveToDate = new Date(moveToDate.getTime() + (30 * 24 * 60 * 60 * 1000));     /// plus 20 days
                            timeline.moveTo(moveToDate, { animation: true });
                        }
                    },


                    right90: {
                        text: '90d >>',
                        class: 'w2ui-btn-grey',
                        style: 'text-transform: uppercase; color: black;  font-size: 12px; ',
                        onClick(event) {
                            // w2alert('Custom Action')
                            moveToDate = new Date(moveToDate.getTime() + (90 * 24 * 60 * 60 * 1000));     /// plus 20 days
                            timeline.moveTo(moveToDate, { animation: true });
                        }
                    },




                }
            });
            initForm();

            $.ajax({
                type: 'GET',
                url: '/booking/getAll',
                contentType: 'application/json',
                success: function (data, status, xhr) {

                    // w2popup.open({
                    //     title: 'GET DATA  ' + status,
                    //     with: 600,
                    //     height: 550,
                    //     body: JSON.stringify(data),
                    //     actions: { Ok: w2popup.close }
                    // });



                    // d.setHours(15, 35, 1);

                    data.forEach((element, index, array) => {

                        console.log(element);


                      
                            items.add({
                                id: element.id,
                                type: 'range',
                                group: element.room,
                                // start: element.startDate,
                                // end: element.endDate,

                                start: convertDate(element.startDate).setHours(0, 0, 0),
                                end: convertDate(element.endDate).setHours(23, 59, 59),

                                // title: element.title,

                                content: element.name,
                                className: element.agency,
                                recordData: element,
                            });

                        
                        // console.log(element.x); // 100, 200, 300
                        // console.log(index); // 0, 1, 2
                        // console.log(array); // same myArray object 3 times
                    });




                }
            });
             w2ui.myForm.on('*', function (event) {
                 console.log('Event: ' + event.type, 'Target: ' + event.target, event);
             });
            w2ui.myForm.on('change', function (event) {


                //  Event: change Target: agency
                if (event != null && event.target == 'agency') {
                    //alert('on change  agency ');
                }


                //  Event: change Target: startDate
                if (event != null && event.target == 'startDate') {
                    w2ui['myForm'].setValue('endDate', event.detail.value.current);
                    // w2ui['myForm'].setValue('numOfNights', 0);
                    w2ui['myForm'].refresh();
                    // console.dir(  w2ui['myForm'].get('endDate')  )  ;
                }

                //  Event: change Target: endDate
                // if (event != null && event.target == 'endDate') {


                //     var startDateFld = w2ui['myForm'].getValue('startDate');
                //     var endDateFld = w2ui['myForm'].getValue('endDate');

                //     var date1 = convertDate_00_00_00(startDateFld);
                //     var date2 = convertDate_00_00_00(endDateFld);

                //     // To calculate the time difference of two dates
                //     var numOfNights = date2.getTime() - date1.getTime();

                //     // To calculate the no. of days between two dates
                //     var numOfNights = numOfNights / (1000 * 3600 * 24);

                //     // set   numOfNights
                //     //  $("#numOfNights").val(numOfNights);

                //     w2ui['myForm'].setValue('numOfNights', numOfNights.toFixed(0));

                //     w2ui['myForm'].refresh();

                //     //  w2ui['form'].set('field_1', { type: 'int' });
                //     if (numOfNights <= 0) {
                //         // w2alert("<b>Error: </b> <br><br> Nights must be more than zero please enter correct dates.");

                //         w2popup.open({
                //             title: 'Message',
                //             with: 300,
                //             height: 150,
                //             body: 'Check your booking dates.',
                //             actions: { Ok: w2popup.close }
                //         });

                //     }

                //     // else {
                //     //     w2alert("Numuber Of Nights is calculated as " + numOfNights + " nigths. <br><br> Start: " + startDateFld + "<br> End: " + endDateFld );
                //     // }
                // }

                // if (event != null && (event.target == 'received' || event.target == 'charge' || event.target == 'commission')) {

                //     var chargeVal = financial(w2ui['myForm'].getValue('charge'));
                //     var receivedVal = financial(w2ui['myForm'].getValue('received'));
                //     var commissionVal = financial(w2ui['myForm'].getValue('commission'));
                //     w2ui['myForm'].refresh();

                //     var newBalance = chargeVal - receivedVal;
                //     var newBalance = newBalance - commissionVal;

                //     w2ui['myForm'].setValue('charge', chargeVal);
                //     w2ui['myForm'].setValue('received', receivedVal);
                //     w2ui['myForm'].setValue('commission', commissionVal);
                //     w2ui['myForm'].setValue('balance', financial(newBalance));

                //     w2ui['myForm'].refresh();

                // }


                w2ui['myForm'].refresh();


            });
            // //  Timeline
            timeline.on('doubleClick', function (properties) {

//                 alert(  JSON.stringify( properties , null, 4)) ;

                if (properties != null && properties.item != null) {

                    var booking = items.get(properties.item);


                    var pBody = '' ;
                    pBody =  pBody +  '<br><br> &nbsp;&nbsp;&nbsp;&nbsp; ' +    booking.recordData.startDate;
                    pBody =  pBody +  ' &nbsp;&nbsp;&nbsp;&nbsp; -  &nbsp;&nbsp;&nbsp;&nbsp;  ' +    booking.recordData.endDate;

                    pBody =  pBody + '<br><br> &nbsp;&nbsp;&nbsp;&nbsp; '+  booking.recordData.name;

                    if (  booking.recordData.extraInfo  != null ){
                        pBody = pBody + '<br> <br> &nbsp;&nbsp;&nbsp;&nbsp; ' +  booking.recordData.extraInfo
                    }

                    w2popup.open({
                         title: ' Selected ',
                         with: 300,
                         height: 350,
                         body:  pBody,
                         actions: { Ok: w2popup.close }
                    });


                    w2ui['myForm'].setValue('name', booking.recordData.name);
                    // w2ui['myForm'].setValue('country', booking.recordData.country);

                    w2ui['myForm'].setValue('startDate', booking.recordData.startDate);
                    w2ui['myForm'].setValue('endDate', booking.recordData.endDate);
                    // w2ui['myForm'].setValue('numOfNights', booking.recordData.numOfNights);

                    // w2ui['myForm'].setValue('balance', financial(booking.recordData.balance));
                    // w2ui['myForm'].setValue('charge', financial(booking.recordData.charge));
                    // w2ui['myForm'].setValue('received', financial(booking.recordData.received));
                    // w2ui['myForm'].setValue('commission', financial(booking.recordData.commission));

                    // w2ui['myForm'].setValue('numOfGuests', booking.recordData.numOfGuests);

                    w2ui['myForm'].setValue('agency', booking.recordData.agency);
                    w2ui['myForm'].setValue('room', booking.recordData.room);

                    // w2ui['myForm'].setValue('voucher', booking.recordData.voucher);
                    // w2ui['myForm'].setValue('email', booking.recordData.email);
                    // w2ui['myForm'].setValue('tel', booking.recordData.tel);
                    // w2ui['myForm'].setValue('identification', booking.recordData.identification);

                    // w2ui['myForm'].setValue('invoiceNumber', booking.recordData.invoiceNumber);
                    // w2ui['myForm'].setValue('taxRefNumber', booking.recordData.taxRefNumber);

                    // w2ui['myForm'].setValue('afm', booking.recordData.afm);

                    w2ui['myForm'].setValue('extraInfo', booking.recordData.extraInfo);


                    w2ui['myForm'].refresh();


                    // todo  load the form with the selected item from the timeline

                }

            });
            // timeline.on('doubleClick', function (properties) {
            //     console.log("Double click event fired");
            //     w2alert('Not me!! The other button');
            // });










});// on ready
//////
