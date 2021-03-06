import { LoggerHelper } from 'helpers/logger.helper';
import { ProgrammingError } from 'error/programming.error';

/**
 * Represents classes that are used to handle deliver_message websocket events
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @abstract
 * @class AbstractWebsocketApplicationHandler
 */
export abstract class AbstractWebsocketApplicationHandler {
    protected _eventsMap: { [eventName: string]: string } = {};

    protected _log: LoggerHelper = new LoggerHelper(this.constructor.name);

    /**
     * Returns the name of the public function used to handle an event
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} eventName Name of the event
     * @returns {string} string name of the local public function
     * @memberof AbstractWebsocketApplicationHandler
     */
    public getHandlerMethod(eventName: string): string {
        const handlerMethod = this._eventsMap[eventName];
        return (handlerMethod && typeof handlerMethod === 'string')
            ? handlerMethod
            : null;
    }

    /**
     * Returns the map of events
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @returns {{ [eventName: string]: string; }} Key is the eventName, and the value is the method name
     * @memberof WebsocketApplicationHandler
     */
    public getEventsMap(): { [eventName: string]: string; } {
        return this._eventsMap;
    }

    /**
     * Executes the action
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {SocketIOClient.Socket} socket The related socket
     * @param {string} eventName name of the event to execute
     * @param {*} content Content sent by the socket
     * @returns {Promise<any>} Promise resolved when the event has been solved inside the method handler
     * @throws {ProgrammingError} When the eventName doesn't have a hander in this websocket handler
     * @memberof WebsocketApplicationHandler
     */
    public async execute(socket: SocketIOClient.Socket, eventName: string, content: any): Promise<any> {
        const functionName: string = this.getHandlerMethod(eventName);
        if (functionName && typeof this[functionName] === 'function') {
            this[functionName](socket, content);
        } else {
            throw new ProgrammingError('Handler for ' + eventName + ' NOT found in ' + this.constructor.name);
        }
    }
}
