
/*------------------------------------------------------------------------
    File        : wcs-server.p
    Purpose     : 

    Syntax      :

    Description : 

    Author(s)   : lucas784
    Created     : Tue Oct 29 17:41:13 CET 2024
    Notes       :
  ----------------------------------------------------------------------*/

/* ***************************  Definitions  ************************** */

block-level on error undo, throw.


define temp-table message-body no-undo
  field msg-id               as int
  field part-id              as int
  field item-name            as character 
  field item-data-type       as integer 
  field item-value           as character 
  field item-bytes-value     as raw 
  field item-stream-sequence as integer 
  field component-sequence   as integer
  .

define input parameter iiPLC as integer.

/* ********************  Preprocessor Definitions  ******************** */

/* ************************  Function Prototypes ********************** */


function convertToHex returns character private
  ( input iiValue as integer ) forward.

function extractMessage returns character private
  (input irData as raw) forward.

define variable cUser          as character no-undo.
define variable cPassword      as character no-undo.
define variable cAdapterURLSMQ as character no-undo.
define variable cFilter        as character no-undo.
define variable cClientID      as character no-undo.
define variable cSendQueue     as character no-undo.
define variable cReceiveQueue  as character no-undo.
define variable cLog           as character no-undo.

define variable hPtp           as handle    no-undo.
define variable hMsgConsumer   as handle    no-undo.
define variable hMsgConsumer2   as handle    no-undo.

define variable iChannel1      as integer   no-undo.
define variable iChannel2      as integer   no-undo.
define variable iChannel3      as integer   no-undo.
/* ***************************  Main Block  *************************** */

case iiPLC:
  when 1 then
    assign
      iChannel1 = 1
      iChannel2 = 2
      iChannel3 = 3.
  when 2 then
    assign
      iChannel1 = 4
      iChannel2 = 5
      iChannel3 = 6.
  when 3 then
    assign
      iChannel1 = 7
      iChannel2 = 8
      iChannel3 = 9.
  when 4 then
    assign
      iChannel1 = 10
      iChannel2 = 11
      iChannel3 = 12.
  when 5 then
    assign
      iChannel1 = 13
      iChannel2 = 14
      iChannel3 = 15.
  when 6 then
    assign
      iChannel1 = 16
      iChannel2 = 17
      iChannel3 = 18.	
  when 7 then
    assign
      iChannel1 = 19
      iChannel2 = 19
      iChannel3 = 19.	  
  otherwise 
  return "PLC not supported!".
end case.


assign
  cUser          = "admin"
  cPassword      = "admin"
  cAdapterURLSMQ = "failover:(amqp://localhost:5672)?amqp.idleTimeout=240000&jms.prefetchPolicy.all=1"   
  cFilter        = substitute("(channel='&1' OR channel='&2' OR channel='&3')", iChannel1, iChannel2, iChannel3)
  cClientID      = "WCS_" + string(iiPLC) + "_" + STRING(now) + STRING(random(1,1000000))
  cSendQueue     = "ToPLC"
  cReceiveQueue  = "FromPLC".                                  
 
run jms/ptpsession.p persistent set hPtp ("-SMQConnect").
run setBrokerUrl in hPtp (cAdapterURLSMQ).

run setClientID in hPtp (cClientID).
run setUser in hPtp ("admin").
run setPassword in hPtp ("admin").
run beginSession in hPtp. 

run createMessageConsumer in hPtp(this-procedure, "readMessageFromQueue",
  output hMsgConsumer).
run ReceiveFromQueue in hPtp (cReceiveQueue, "" ,hMsgConsumer).

 
 
run StartReceiveMessages in hPtp.
  
repeat:
  
  process events.
  
  pause 0.001 no-message.
   
end.

/* **********************  Internal Procedures  *********************** */

 
procedure readMessageFromQueue:
  /*------------------------------------------------------------------------------
   Purpose:
   Notes:
  ------------------------------------------------------------------------------*/
 define input  parameter hMessage     as handle no-undo.
  define input  parameter hMsgConsumer as handle no-undo.
  define output parameter hReply       as handle no-undo.
  
  define variable lcJson   as longchar  no-undo.
  define variable cChannel as character no-undo.
  define variable mMessage as memptr    no-undo.

  lcJson  = dynamic-function("getText":U in hMessage).
    
  display string(lcJson) format "X(32)" with 2 col.
  
  output to value("C:\temp\wcs.log") append.
  put unformatted now " " dynamic-function("getCharProperty" in hMessage, "channel") " " string(lcjson) skip.
  output close.
  
  cChannel = dynamic-function("getCharProperty" in hMessage, "channel").
  
  run deleteMessage in hMessage. 
 
  RUN createTextMessage IN hPTP (OUTPUT hMessage).
  run clearbody in hMessage. 
  RUN setText IN hMessage (lcjson).
  run clearproperties in hMessage. 
  
  run setStringProperty in hMessage ("channel", cChannel). 
    
  RUN sendToQueue IN hPtp (
    cSendQueue,        /* Name of the queue */
    hMessage,       /* Message object */
    ?,              /* Message priority: 0-9 (optional) */
    0,              /* TimeToLive in milliseconds (optional) */
    "PERSISTENT"           /* Delivery mode (optional); NON-PERSISTENT : */ 
    /* Messages doesn't stay on the queue */
    /* after a shutdown of the sonicMQ Broker.*/
    ).
  
  run deleteMessage in hMessage.
  
end procedure.

procedure readMessageFromRabbitMQ1:
define input  parameter hMessage     as handle no-undo.
  define input  parameter hMsgConsumer as handle no-undo.
  define output parameter hReply       as handle no-undo.
  
  run readMessageFromRabbitMQ("80" + string(iChannel1, "99"), hMessage, hMsgConsumer).
  
end procedure.

procedure readMessageFromRabbitMQ2:
define input  parameter hMessage     as handle no-undo.
  define input  parameter hMsgConsumer as handle no-undo.
  define output parameter hReply       as handle no-undo.
  
  run readMessageFromRabbitMQ("80" + string(iChannel2, "99"), hMessage, hMsgConsumer).

end procedure.

procedure readMessageFromRabbitMQ3:
define input  parameter hMessage     as handle no-undo.
  define input  parameter hMsgConsumer as handle no-undo.
  define output parameter hReply       as handle no-undo.
  
  run readMessageFromRabbitMQ("80" + string(iChannel3, "99"), hMessage, hMsgConsumer).

end procedure.

procedure readMessageFromRabbitMQ:
  /*------------------------------------------------------------------------------
   Purpose:
   Notes:
  ------------------------------------------------------------------------------*/
  define input  parameter icChannel    as character no-undo.
  define input  parameter hMessage     as handle  no-undo.
  define input  parameter hMsgConsumer as handle  no-undo. 
  
   
  define variable cChannel as character no-undo.
  define variable mMessage as memptr    no-undo.
  define variable lcJson   as longchar  no-undo.

  run getMessageBody in hMessage(output table message-body).
  
  
  for each message-body:
    lcJson = extractMessage(message-body.item-bytes-value).
  end.
        
  display string(lcJson) format "X(32)"with 2 col.
  
  output to value("C:\temp\wcs.log") append.
  put unformatted now     icChannel       string(lcjson) skip.
  output close.
  
  
  run deleteMessage in hMessage.
  
  RUN createBytesMessage IN hPtp (OUTPUT hMessage).
  copy-lob lcJson to mMessage.
  RUN SetMemPtr IN hMessage (mMessage,1,GET-SIZE(mMessage)).
   
  
  RUN sendToQueue IN hPtp (
    cSendQueue + icChannel,        /* Name of the queue */
    hMessage,       /* Message object */
    1,              /* Message priority: 0-9 (optional) */
    50000,              /* TimeToLive in milliseconds (optional) */
    "PERSISTENT"           /* Delivery mode (optional); NON-PERSISTENT : */ 
    /* Messages doesn't stay on the queue */
    /* after a shutdown of the sonicMQ Broker.*/
    ).
     
  run deleteMessage in hMessage.
  
  CATCH e AS Progress.Lang.Error:
    MESSAGE "Error sending message to queue: " + e:GetMessage(1) VIEW-AS ALERT-BOX.
  END.

end procedure.

/* ************************  Function Implementations ***************** */

 

function extractMessage returns character private
  ( input irData as raw ):
  /*------------------------------------------------------------------------------
   Purpose:
   Notes:
  ------------------------------------------------------------------------------*/  
  define variable cHexString as character no-undo.
  define variable i          as integer   no-undo.
  define variable ibyteValue as integer   no-undo. 

  define variable c1         as character.
  define variable c2         as character.
  def var m as memptr no-undo.
 
  m = irData.
  i = get-size(m) .
  do i = 1 to get-size(m) by 2 :
   c1 = get-string(m, i, 2).
	 
    cHexString = cHexString + c1 .
   end.
  
  return cHexString.

    
end function