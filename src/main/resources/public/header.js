
    <meta charset="utf-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta name="description" content="" />
    <meta name="viewport" content="width=device-width" />
    <!-- <base href="/" /> -->
    <!-- <link href="https://rawgit.com/vitmalina/w2ui/master/dist/w2ui.css" rel="stylesheet" type="text/css" />
    <link href="https://unpkg.com/vis-timeline@latest/styles/vis-timeline-graph2d.min.css" rel="stylesheet"
        type="text/css" /> -->
    <link href="w2ui.css" rel="stylesheet" type="text/css" />
    <link href="vis-timeline-graph2d.min.css" rel="stylesheet" type="text/css" />

    <style>
        .vis-item.bookingcom {
            background-color: rgb(166, 199, 243);
            border-color: rgb(22, 2, 59);
            color: rgb(1, 26, 29);
        }

        .vis-item.walkin {
            background-color: rgb(114, 199, 114);
            border-color: rgb(23, 32, 23);
            color: rgb(21, 22, 18);
        }

        .vis-item.abnb {
            background-color: rgb(247, 166, 166);
            border-color: rgb(39, 3, 27);
            color: rgb(27, 6, 46);
        }


        .vis-item.closed {
            background-color: black;
            border-color: black;
            color: rgb(255, 255, 255);
        }


        .vis-item.vis-selected {
            color: black;
        }


        /* alternating column backgrounds */
        .vis-time-axis .vis-grid.vis-odd {
            background: #f5f5f5;
        }

        .w2ui-input[readonly],
        .w2ui-input[disabled] {
            background-image: none;
            background-color: #ffffff !important;
            color: #09053f !important;
        }

        .w2ui-input {
            background-image: none;
            background-color: #ffffff !important;
            color: #09053f !important;
        }
    </style>


    <!-- <script src="http://ajax.googleapis.com/ajax/libs/jquery/3.5.0/jquery.min.js" type="text/javascript"></script>
    <script src="https://rawgit.com/vitmalina/w2ui/master/dist/w2ui.js" type="text/javascript"></script>
    <script src="https://unpkg.com/vis-timeline@latest/standalone/umd/vis-timeline-graph2d.min.js"
        type="text/javascript"></script> -->

    <script src="jquery.min.js" type="text/javascript"></script>
    <script src="w2ui.js" type="text/javascript"></script>
    <script src="vis-timeline-graph2d.min.js" type="text/javascript"></script>