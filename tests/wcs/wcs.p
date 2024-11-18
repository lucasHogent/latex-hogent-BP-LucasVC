
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

define input parameter iiPLC as integer.

/* ********************  Preprocessor Definitions  ******************** */

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
 
run jms/ptpsession.p persistent set hPtp ("-SMQConnect").
run setBrokerUrl in hPtp ("failover:(amqp://localhost:5672)?amqp.idleTimeout=240000&jms.prefetchPolicy.all=1").

run setClientID in hPtp (cClientID).
run setUser in hPtp ("admin").
run setPassword in hPtp ("admin").
run beginSession in hPtp. 
      
RUN createMessageConsumer IN hPtp(THIS-PROCEDURE, "readMessageFromQueue",
  OUTPUT hMsgConsumer).
RUN ReceiveFromQueue IN hPtp (cReceiveQueue, "" ,hMsgConsumer).
RUN StartReceiveMessages IN hPtp.
  
repeat:
  
  process events.
  
  pause 0.5.
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
  
  define variable lcJson as longchar no-undo.

  lcJson  = DYNAMIC-FUNCTION("getText":U IN hMessage).
    
  display string(lcJson)with 2 col.
  
  output to value("C:\temp\wcs.log") append.
  put unformatted now string(lcjson) skip.
  output close.
    
  run deleteMessage in hMessage.
        
end procedure.
            
