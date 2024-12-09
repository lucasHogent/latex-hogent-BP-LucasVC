
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
  otherwise 
  return "PLC not supported!".
end case.


assign
  cUser          = "admin"
  cPassword      = "admin"
  cAdapterURLSMQ = "failover:(amqp://localhost:5672)?amqp.idleTimeout=240000&jms.prefetchPolicy.all=1"   
  cFilter        = substitute("(channel='&1' OR channel='&2' OR channel='&3')", iChannel1, iChannel2, iChannel3)
  cClientID      = "WCS_" + string(iiPLC) + STRING(now) + STRING(random(1,1000000))
  cSendQueue     = "ToPLC"
  cReceiveQueue  = "FromPLC".                                  
 
run jms/jmssession.p persistent set hPtp ("-SMQConnect").
run setBrokerUrl in hPtp (cAdapterURLSMQ).

run setClientID in hPtp (cClientID).
run setUser in hPtp ("admin").
run setPassword in hPtp ("admin").
run beginSession in hPtp. 

   
run createMessageConsumer in hPtp(this-procedure, "readMessageFromRabbitMQ",
  output hMsgConsumer).
run ReceiveFromQueue in hPtp (cReceiveQueue, "" ,hMsgConsumer).
 message hMsgConsumer:internal-entries view-as alert-box.
   
run StartReceiveMessages in hPtp.
  
repeat:
  
  process events.
  
  pause 0.1.
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

  lcJson  = dynamic-function("getText":U in hMessage).
    
  display string(lcJson)with 2 col.
  
  output to value("C:\temp\wcs.log") append.
  put unformatted now dynamic-function("getCharProperty" in hMessage, "channel") string(lcjson) skip.
  output close.
  
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

procedure readMessageFromRabbitMQ:
  /*------------------------------------------------------------------------------
   Purpose:
   Notes:
  ------------------------------------------------------------------------------*/
  define input  parameter hMessage     as handle no-undo.
  define input  parameter hMsgConsumer as handle no-undo.
  define output parameter hReply       as handle no-undo.
  
  define variable cChannel as character no-undo.
  define variable lcJson   as longchar  no-undo.

  run getMessageBody in hMessage(output table message-body).
  
  for each message-body:
    lcJson = extractMessage(message-body.item-bytes-value).
  end.
  
      
  display string(lcJson)with 2 col.
  
  output to value("C:\temp\wcs.log") append.
  put unformatted now dynamic-function("getCharProperty" in hMessage, "channel") string(lcjson) skip.
  output close.
  
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

/* ************************  Function Implementations ***************** */

function convertToHex returns character private
  ( input iiValue as integer ):
  /*------------------------------------------------------------------------------
   Purpose:
   Notes:
  ------------------------------------------------------------------------------*/  
  case iiValue:
    when 10 then 
      return "A".
    when 11 then 
      return "B".
    when 12 then 
      return "C".
    when 13 then 
      return "D".
    when 14 then 
      return "E".
    when 15 then 
      return "F".
    otherwise 
    return string(iiValue).
  end case.
    
end function.

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
 
 
  
  do i = 1 to length(irData) + 2:

    ibyteValue = get-byte(irData, i).
    c1 = string(ibyteValue modulo 16).
    if ibyteValue > 16 then
      c2 = string(ibyteValue / 16 ).
    else c2 = "0"  .
   
    cHexString = cHexString + convertToHex(int(c2)) + convertToHex(int(c1)) + " ".
  end.
  
  return cHexString.

    
end function